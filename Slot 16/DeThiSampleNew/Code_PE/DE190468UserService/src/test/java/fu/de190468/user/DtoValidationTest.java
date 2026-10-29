package fu.de190468.user;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.user.common.Create;
import fu.de190468.user.dto.UserDTO;
import jakarta.validation.*;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class DtoValidationTest {
  static ValidatorFactory factory;
  static Validator validator;

  @BeforeAll
  static void init() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void close() {
    factory.close();
  }

  UserDTO valid() {
    var d = new UserDTO();
    d.setUsername("creator");
    d.setEmail("a@example.com");
    d.setRole("CREATOR");
    d.setRatePerPost(new BigDecimal("500000"));
    d.setMaxDailyPosts(3);
    d.setLevel(1);
    return d;
  }

  @Test
  void completeCreateIsValid() {
    assertTrue(validator.validate(valid(), Create.class).isEmpty());
  }

  @Test
  void createRequiresAllMandatoryFields() {
    assertEquals(6, validator.validate(new UserDTO(), Create.class).size());
  }

  @Test
  void partialUpdateAllowsOmittedFields() {
    assertTrue(validator.validate(new UserDTO()).isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFields")
  void invalidFieldsAreRejectedForCreateAndUpdate(String label, Consumer<UserDTO> mutate) {
    var dto = valid();
    mutate.accept(dto);
    assertFalse(validator.validate(dto, Create.class).isEmpty(), label);
    assertFalse(validator.validate(dto).isEmpty(), label);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("username blank", (Consumer<UserDTO>) d -> d.setUsername(" ")),
        Arguments.of("username long", (Consumer<UserDTO>) d -> d.setUsername("x".repeat(31))),
        Arguments.of("email invalid", (Consumer<UserDTO>) d -> d.setEmail("bad")),
        Arguments.of("role invalid", (Consumer<UserDTO>) d -> d.setRole("BAD")),
        Arguments.of("rate zero", (Consumer<UserDTO>) d -> d.setRatePerPost(BigDecimal.ZERO)),
        Arguments.of(
            "rate precision", (Consumer<UserDTO>) d -> d.setRatePerPost(new BigDecimal("1.123"))),
        Arguments.of("daily posts zero", (Consumer<UserDTO>) d -> d.setMaxDailyPosts(0)),
        Arguments.of("daily posts eleven", (Consumer<UserDTO>) d -> d.setMaxDailyPosts(11)),
        Arguments.of("level zero", (Consumer<UserDTO>) d -> d.setLevel(0)),
        Arguments.of("status invalid", (Consumer<UserDTO>) d -> d.setStatus("BAD")));
  }
}
