package fu.de190468.room;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.room.common.Create;
import fu.de190468.room.dto.RoomDTO;
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

  RoomDTO valid() {
    var d = new RoomDTO();
    d.setRoomNumber("A-101");
    d.setRoomType("DOUBLE");
    d.setPricePerNight(new BigDecimal("850000"));
    d.setCapacity(2);
    d.setFloor(1);
    return d;
  }

  @Test
  void completeCreateIsValid() {
    assertTrue(validator.validate(valid(), Create.class).isEmpty());
  }

  @Test
  void createRequiresAllMandatoryFields() {
    assertEquals(5, validator.validate(new RoomDTO(), Create.class).size());
  }

  @Test
  void partialUpdateAllowsOmittedFields() {
    assertTrue(validator.validate(new RoomDTO()).isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFields")
  void invalidFieldsAreRejectedForCreateAndUpdate(String label, Consumer<RoomDTO> mutate) {
    var dto = valid();
    mutate.accept(dto);
    assertFalse(validator.validate(dto, Create.class).isEmpty(), label);
    assertFalse(validator.validate(dto).isEmpty(), label);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("number blank", (Consumer<RoomDTO>) d -> d.setRoomNumber(" ")),
        Arguments.of("number long", (Consumer<RoomDTO>) d -> d.setRoomNumber("x".repeat(21))),
        Arguments.of("type invalid", (Consumer<RoomDTO>) d -> d.setRoomType("BAD")),
        Arguments.of("price zero", (Consumer<RoomDTO>) d -> d.setPricePerNight(BigDecimal.ZERO)),
        Arguments.of(
            "price precision",
            (Consumer<RoomDTO>) d -> d.setPricePerNight(new BigDecimal("1.123"))),
        Arguments.of("capacity zero", (Consumer<RoomDTO>) d -> d.setCapacity(0)),
        Arguments.of("capacity eleven", (Consumer<RoomDTO>) d -> d.setCapacity(11)),
        Arguments.of("floor zero", (Consumer<RoomDTO>) d -> d.setFloor(0)),
        Arguments.of("status invalid", (Consumer<RoomDTO>) d -> d.setStatus("BAD")));
  }
}
