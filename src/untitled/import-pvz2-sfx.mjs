import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const albumUrl = "https://downloads.khinsider.com/game-soundtracks/album/plants-vs-zombies-2-mobile-gamerip-2013";
const categories = new Map([
    [2, "voices"],
    [3, "others"],
    [4, "plants"],
    [5, "zombies"]
]);
const projectDirectory = path.dirname(fileURLToPath(import.meta.url));
const outputDirectory = path.resolve(projectDirectory, "assets/audio/sfx");
const concurrency = 8;

function extractPackedScript(html) {
    const match = html.match(/<script>(eval\(function\(p,a,c,k,e,d\)[\s\S]*?)<\/script>/);
    if (!match) {
        throw new Error("Could not find the soundtrack data");
    }
    return match[1].trim().replace(/;$/, "");
}

function unpackScript(packedScript) {
    if (!packedScript.startsWith("eval(") || !packedScript.endsWith(")")) {
        throw new Error("Unexpected soundtrack data format");
    }
    const expression = packedScript.slice(5, -1);
    return vm.runInNewContext(`(${expression})`, Object.create(null), { timeout: 10_000 });
}

function findTrackArray(source) {
    let start = source.indexOf('[{"');
    while (start >= 0) {
        let depth = 0;
        let quoted = false;
        let escaped = false;
        for (let index = start; index < source.length; index++) {
            const character = source[index];
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (character === "\\") {
                    escaped = true;
                } else if (character === '"') {
                    quoted = false;
                }
                continue;
            }
            if (character === '"') {
                quoted = true;
            } else if (character === "[") {
                depth++;
            } else if (character === "]" && --depth === 0) {
                try {
                    const arraySource = source.slice(start, index + 1);
                    const value = vm.runInNewContext(`(${arraySource})`, Object.create(null), { timeout: 10_000 });
                    if (Array.isArray(value) && value.length > 5_000) {
                        return value;
                    }
                } catch {
                }
                break;
            }
        }
        start = source.indexOf('[{"', start + 2);
    }
    throw new Error("Could not read the soundtrack track list");
}

function trackFrom(rawTrack) {
    const values = Object.values(rawTrack);
    const sourcePath = values.find(value => typeof value === "string" && value.endsWith(".mp3") && value.includes("/"));
    if (!sourcePath) {
        return null;
    }
    const sourceUrl = sourcePath.startsWith("http") ? sourcePath : `https://${sourcePath}`;
    const encodedName = sourcePath.slice(sourcePath.lastIndexOf("/") + 1);
    const decodedName = decodeURIComponent(encodedName);
    const match = decodedName.match(/^([1-5])-(\d+)\.\s*(.+)\.mp3$/i);
    if (!match) {
        return null;
    }
    const cd = Number(match[1]);
    const category = categories.get(cd);
    if (!category) {
        return null;
    }
    const title = match[3].trim();
    const safeTitle = title
        .normalize("NFKD")
        .replace(/[^a-zA-Z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "")
        .toLowerCase()
        .slice(0, 96) || "sound";
    const number = match[2].padStart(4, "0");
    const fileName = `${number}_${safeTitle}.mp3`;
    return {
        cd,
        number: Number(match[2]),
        category,
        title,
        sourceUrl,
        assetPath: `audio/sfx/${category}/${fileName}`,
        outputPath: path.join(outputDirectory, category, fileName)
    };
}

async function fetchWithRetry(url, attempts = 4) {
    let lastError;
    for (let attempt = 1; attempt <= attempts; attempt++) {
        try {
            const response = await fetch(url, {
                headers: { "User-Agent": "PvZ2 university project asset importer" }
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return response;
        } catch (error) {
            lastError = error;
            if (attempt < attempts) {
                await new Promise(resolve => setTimeout(resolve, attempt * 750));
            }
        }
    }
    throw lastError;
}

async function download(track) {
    if (fs.existsSync(track.outputPath) && fs.statSync(track.outputPath).size > 0) {
        return false;
    }
    const response = await fetchWithRetry(track.sourceUrl);
    const data = Buffer.from(await response.arrayBuffer());
    if (data.length === 0) {
        throw new Error("Downloaded an empty file");
    }
    fs.mkdirSync(path.dirname(track.outputPath), { recursive: true });
    fs.writeFileSync(track.outputPath, data);
    return true;
}

async function runWorkers(tracks) {
    let nextIndex = 0;
    let downloaded = 0;
    let skipped = 0;
    const failures = [];

    async function worker() {
        while (true) {
            const index = nextIndex++;
            if (index >= tracks.length) {
                return;
            }
            const track = tracks[index];
            try {
                if (await download(track)) {
                    downloaded++;
                } else {
                    skipped++;
                }
            } catch (error) {
                failures.push({ track, message: error.message });
            }
            const completed = downloaded + skipped + failures.length;
            if (completed % 100 === 0 || completed === tracks.length) {
                console.log(`${completed}/${tracks.length} downloaded=${downloaded} skipped=${skipped} failed=${failures.length}`);
            }
        }
    }

    await Promise.all(Array.from({ length: concurrency }, worker));
    return failures;
}

async function main() {
    fs.mkdirSync(outputDirectory, { recursive: true });
    const albumResponse = await fetchWithRetry(albumUrl);
    const albumHtml = await albumResponse.text();
    const rawTracks = findTrackArray(unpackScript(extractPackedScript(albumHtml)));
    const tracks = rawTracks
        .map(trackFrom)
        .filter(Boolean)
        .sort((left, right) => left.cd - right.cd || left.number - right.number);

    if (tracks.length !== 5_167) {
        throw new Error(`Expected 5167 SFX files but found ${tracks.length}`);
    }

    const manifest = tracks.map(({ cd, number, category, title, sourceUrl, assetPath }) => ({
        cd,
        number,
        category,
        title,
        sourceUrl,
        assetPath
    }));
    fs.writeFileSync(
        path.join(outputDirectory, "manifest.json"),
        `${JSON.stringify({ source: albumUrl, tracks: manifest }, null, 2)}\n`,
        "utf8"
    );

    console.log(`Importing ${tracks.length} SFX files into ${outputDirectory}`);
    const failures = await runWorkers(tracks);
    if (failures.length > 0) {
        fs.writeFileSync(
            path.join(outputDirectory, "failed-downloads.json"),
            `${JSON.stringify(failures, null, 2)}\n`,
            "utf8"
        );
        throw new Error(`${failures.length} files could not be downloaded`);
    }
    fs.rmSync(path.join(outputDirectory, "failed-downloads.json"), { force: true });
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
