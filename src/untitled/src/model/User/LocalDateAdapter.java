package model.User;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class LocalDateAdapter
        extends TypeAdapter<LocalDate> {

    @Override
    public void write(
            JsonWriter writer,
            LocalDate date
    ) throws IOException {

        if (date == null) {
            writer.nullValue();
            return;
        }

        writer.value(date.toString());
    }

    @Override
    public LocalDate read(
            JsonReader reader
    ) throws IOException {

        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        String dateText = reader.nextString();

        try {
            return LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            throw new IOException(
                    "Invalid LocalDate value: "
                            + dateText,
                    exception
            );
        }
    }
}