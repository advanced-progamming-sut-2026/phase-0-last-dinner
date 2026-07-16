import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.PasswordHasher;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AccountServiceTest {
    private Path storagePath;
    private UserRepository repository;
    private AccountService service;

    @Before
    public void setUp() throws IOException {
        this.storagePath = Files.createTempDirectory("pvz-users").resolve("users.json");
        this.repository = new UserRepository(this.storagePath);
        this.service = new AccountService(this.repository);
    }

    @Test
    public void registrationIsTwoStepAndPersistsHashedCredentials() {
        AccountResult pending = this.service.beginRegistration(
                "shayan-1",
                "Strong#123",
                "Strong#123",
                "Shayan",
                "shayan@example.com",
                UserGender.MALE
        );

        assertEquals(AccountStatus.SECURITY_QUESTION_REQUIRED, pending.getStatus());
        assertTrue(this.repository.getUsers().isEmpty());

        AccountResult completed = this.service.completeRegistration(1, "blue", "blue");
        assertTrue(completed.isSuccessful());
        assertEquals(1, this.repository.getUsers().size());
        assertFalse("Strong#123".equals(completed.getUser().getHashedPassword()));
        assertEquals(64, completed.getUser().getHashedPassword().length());

        UserRepository loaded = new UserRepository(this.storagePath);
        User stored = loaded.findByUsername("shayan-1");
        assertNotNull(stored);
        assertEquals(PasswordHasher.hash("Strong#123"), stored.getHashedPassword());
        assertEquals(PasswordHasher.hash("blue"), stored.getSecurityAnswer());
    }

    @Test
    public void invalidRegistrationDoesNotCreateUser() {
        AccountResult invalidUsername = this.service.beginRegistration(
                "bad user",
                "Strong#123",
                "Strong#123",
                "Shayan",
                "shayan@example.com",
                UserGender.MALE
        );
        assertEquals(AccountStatus.USERNAME_INVALID, invalidUsername.getStatus());

        AccountResult weakPassword = this.service.beginRegistration(
                "shayan",
                "weak",
                "weak",
                "Shayan",
                "shayan@example.com",
                UserGender.MALE
        );
        assertEquals(AccountStatus.PASSWORD_INVALID, weakPassword.getStatus());

        AccountResult invalidEmail = this.service.beginRegistration(
                "shayan",
                "Strong#123",
                "Strong#123",
                "Shayan",
                "john..doe@example.com",
                UserGender.MALE
        );
        assertEquals(AccountStatus.EMAIL_INVALID, invalidEmail.getStatus());
        assertTrue(this.repository.getUsers().isEmpty());
    }

    @Test
    public void duplicateUsernameIsRejectedIgnoringCase() {
        this.registerUser();

        AccountResult result = this.service.beginRegistration(
                "SHAYAN",
                "Other#123",
                "Other#123",
                "Other",
                "other@example.com",
                UserGender.FEMALE
        );

        assertEquals(AccountStatus.USERNAME_TAKEN, result.getStatus());
    }

    @Test
    public void rejectsAllInvalidEmailExamplesFromDocument() {
        String[] invalidEmails = {
                "john..doe@example.com",
                "user@domain",
                "user@domain.c",
                "user@domain..com",
                "user@.com"
        };

        for (String email : invalidEmails) {
            AccountResult result = this.service.beginRegistration(
                    "shayan",
                    "Strong#123",
                    "Strong#123",
                    "Shayan",
                    email,
                    UserGender.MALE
            );
            assertEquals(email, AccountStatus.EMAIL_INVALID, result.getStatus());
        }

        assertTrue(this.repository.getUsers().isEmpty());
    }

    @Test
    public void stayLoggedInAndPasswordRecoverySurviveReload() {
        this.registerUser();

        AccountResult login = this.service.login("shayan", "Strong#123", true);
        assertTrue(login.isSuccessful());

        UserRepository loaded = new UserRepository(this.storagePath);
        assertNotNull(loaded.getRememberedUser());

        AccountService loadedService = new AccountService(loaded);
        AccountResult question = loadedService.beginPasswordReset("shayan", "shayan@example.com");
        assertTrue(question.isSuccessful());
        assertNotNull(question.getSecurityQuestion());

        AccountResult reset = loadedService.completePasswordReset("blue", "NewPass#456", "NewPass#456");
        assertTrue(reset.isSuccessful());
        assertTrue(loadedService.login("shayan", "NewPass#456", false).isSuccessful());
        assertNull(new UserRepository(this.storagePath).getRememberedUser());
    }

    private void registerUser() {
        this.service.beginRegistration(
                "shayan",
                "Strong#123",
                "Strong#123",
                "Shayan",
                "shayan@example.com",
                UserGender.MALE
        );
        this.service.completeRegistration(1, "blue", "blue");
    }
}
