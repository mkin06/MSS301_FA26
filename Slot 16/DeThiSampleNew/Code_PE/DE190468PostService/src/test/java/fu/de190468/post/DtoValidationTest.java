package fu.de190468.post;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.post.common.Create;
import fu.de190468.post.dto.PostDTO;
import jakarta.validation.*;
import java.util.Date;
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

  PostDTO valid() {
    var d = new PostDTO();
    d.setAdvertiserName("Shop");
    d.setAdvertiserEmail("a@example.com");
    d.setAdvertiserPhone("0900000000");
    d.setStartDate(new Date(0));
    d.setEndDate(new Date(86400000));
    d.setNumberOfPosts(1);
    d.setUserId(1L);
    return d;
  }

  @Test
  void completeCreateIsValid() {
    assertTrue(validator.validate(valid(), Create.class).isEmpty());
  }

  @Test
  void createRequiresAllMandatoryFields() {
    assertEquals(7, validator.validate(new PostDTO(), Create.class).size());
  }

  @Test
  void partialUpdateAllowsOmittedFields() {
    assertTrue(validator.validate(new PostDTO()).isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFields")
  void invalidFieldsAreRejectedForCreateAndUpdate(String label, Consumer<PostDTO> mutate) {
    var dto = valid();
    mutate.accept(dto);
    assertFalse(validator.validate(dto, Create.class).isEmpty(), label);
    assertFalse(validator.validate(dto).isEmpty(), label);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("name blank", (Consumer<PostDTO>) d -> d.setAdvertiserName(" ")),
        Arguments.of("name long", (Consumer<PostDTO>) d -> d.setAdvertiserName("x".repeat(101))),
        Arguments.of("email invalid", (Consumer<PostDTO>) d -> d.setAdvertiserEmail("bad")),
        Arguments.of("phone long", (Consumer<PostDTO>) d -> d.setAdvertiserPhone("0".repeat(21))),
        Arguments.of("posts zero", (Consumer<PostDTO>) d -> d.setNumberOfPosts(0)),
        Arguments.of("user id zero", (Consumer<PostDTO>) d -> d.setUserId(0L)),
        Arguments.of("status invalid", (Consumer<PostDTO>) d -> d.setStatus("BAD")));
  }
}
