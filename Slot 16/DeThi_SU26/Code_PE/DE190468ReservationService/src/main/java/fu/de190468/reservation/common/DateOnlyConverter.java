package fu.de190468.reservation.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/** Preserve SQL DATE calendar days independently of the server time zone. */
@Converter
public class DateOnlyConverter implements AttributeConverter<Date, LocalDate> {
  @Override
  public LocalDate convertToDatabaseColumn(Date value) {
    return value == null
        ? null
        : new Date(value.getTime()).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
  }

  @Override
  public Date convertToEntityAttribute(LocalDate value) {
    return value == null ? null : Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
  }
}
