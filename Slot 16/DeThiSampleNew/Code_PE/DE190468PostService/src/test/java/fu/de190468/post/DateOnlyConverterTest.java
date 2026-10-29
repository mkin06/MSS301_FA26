package fu.de190468.post;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.post.common.DateOnlyConverter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class DateOnlyConverterTest {
  @Test
  void calendarDayRoundTripsInDifferentServerTimeZones() {
    TimeZone original = TimeZone.getDefault();
    try {
      var converter = new DateOnlyConverter();
      var expected = Date.from(Instant.parse("2026-07-25T00:00:00Z"));
      for (String zone : new String[] {"Asia/Bangkok", "America/New_York", "UTC"}) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        assertEquals(LocalDate.of(2026, 7, 25), converter.convertToDatabaseColumn(expected));
        assertEquals(expected, converter.convertToEntityAttribute(LocalDate.of(2026, 7, 25)));
      }
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void nullableConversionIsSupported() {
    var converter = new DateOnlyConverter();
    assertNull(converter.convertToDatabaseColumn(null));
    assertNull(converter.convertToEntityAttribute(null));
  }
}
