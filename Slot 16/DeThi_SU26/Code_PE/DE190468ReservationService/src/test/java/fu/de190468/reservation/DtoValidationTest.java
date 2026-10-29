package fu.de190468.reservation;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.reservation.common.Create;
import fu.de190468.reservation.dto.ReservationDTO;
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

  ReservationDTO valid() {
    var d = new ReservationDTO();
    d.setGuestName("Guest");
    d.setGuestEmail("a@example.com");
    d.setGuestPhone("0900000000");
    d.setCheckInDate(new Date(0));
    d.setCheckOutDate(new Date(86400000));
    d.setNumberOfGuests(1);
    d.setRoomId(1L);
    return d;
  }

  @Test
  void completeCreateIsValid() {
    assertTrue(validator.validate(valid(), Create.class).isEmpty());
  }

  @Test
  void createRequiresAllMandatoryFields() {
    assertEquals(7, validator.validate(new ReservationDTO(), Create.class).size());
  }

  @Test
  void partialUpdateAllowsOmittedFields() {
    assertTrue(validator.validate(new ReservationDTO()).isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFields")
  void invalidFieldsAreRejectedForCreateAndUpdate(String label, Consumer<ReservationDTO> mutate) {
    var dto = valid();
    mutate.accept(dto);
    assertFalse(validator.validate(dto, Create.class).isEmpty(), label);
    assertFalse(validator.validate(dto).isEmpty(), label);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("name blank", (Consumer<ReservationDTO>) d -> d.setGuestName(" ")),
        Arguments.of("name long", (Consumer<ReservationDTO>) d -> d.setGuestName("x".repeat(101))),
        Arguments.of("email invalid", (Consumer<ReservationDTO>) d -> d.setGuestEmail("bad")),
        Arguments.of("phone long", (Consumer<ReservationDTO>) d -> d.setGuestPhone("0".repeat(21))),
        Arguments.of("guests zero", (Consumer<ReservationDTO>) d -> d.setNumberOfGuests(0)),
        Arguments.of("guests eleven", (Consumer<ReservationDTO>) d -> d.setNumberOfGuests(11)),
        Arguments.of("room id zero", (Consumer<ReservationDTO>) d -> d.setRoomId(0L)),
        Arguments.of("status invalid", (Consumer<ReservationDTO>) d -> d.setStatus("BAD")));
  }
}
