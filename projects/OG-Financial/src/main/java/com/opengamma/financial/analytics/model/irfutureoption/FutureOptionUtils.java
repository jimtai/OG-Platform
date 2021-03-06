/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.irfutureoption;

import javax.time.calendar.DateAdjuster;
import javax.time.calendar.DateAdjusters;
import javax.time.calendar.DayOfWeek;
import javax.time.calendar.LocalDate;

import org.apache.commons.lang.Validate;

import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.convention.IMMFutureAndFutureOptionQuarterlyExpiryCalculator;
import com.opengamma.financial.convention.calendar.Calendar;

/**
 * Utility Class for computing Expiries of IR Future Options from ordinals (i.e. nth future after valuationDate)
 */
public class FutureOptionUtils {
  private static final DateAdjuster THIRD_WED_ADJUSTER = DateAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY);

  /**
   * Compute time between now and future or future option's settlement date,
   * typically two business days before the third Wednesday of the expiry month.
   * @param n nth Future after now
   * @param today Valuation Date
   * @param holidayCalendar The holiday calendar
   * @return OG-Analytic Time in years between now and the future's settlement date
   */
  public static Double getIRFutureOptionTtm(final int n, final LocalDate today, final Calendar holidayCalendar) {
    final LocalDate expiry = getIRFutureOptionWithSerialOptionsExpiry(n, today, holidayCalendar);
    return TimeCalculator.getTimeBetween(today, expiry);
  }

  /**
   * Compute time between now and future or future option's settlement date,
   * typically two business days before the third Wednesday of the expiry month.
   * @param n nth Future after now
   * @param today Valuation Date
   * @param holidayCalendar The holiday calendar
   * @return OG-Analytic Time in years between now and the future's settlement date
   */
  public static Double getIRFutureTtm(final int n, final LocalDate today, final Calendar holidayCalendar) {
    final LocalDate expiry = getIRFutureQuarterlyExpiryDate(n, today, holidayCalendar);
    return TimeCalculator.getTimeBetween(today, expiry);
  }

  public static LocalDate getIRFutureOptionWithSerialOptionsExpiry(final int nthFuture, final LocalDate valDate, final Calendar holidayCalendar) {
    Validate.isTrue(nthFuture > 0, "nthFuture must be greater than 0.");
    if (nthFuture <= 6) { // We look for expiries in the first 6 serial months after curveDate
      final LocalDate expiry = getIRFutureMonthlyExpiry(nthFuture, valDate);
      final LocalDate previousMonday = expiry.minusDays(2);
      return previousMonday;
    }   // And for Quarterly expiries thereafter
    final int nthExpiryAfterSixMonths = nthFuture - 6;
    final LocalDate sixMonthsForward = valDate.plusMonths(6);
    return getIRFutureQuarterlyExpiryDate(nthExpiryAfterSixMonths, sixMonthsForward, holidayCalendar);
  }

  public static LocalDate getApproximateIRFutureOptionWithSerialOptionsExpiry(final int nthFuture, final LocalDate valDate) {
    Validate.isTrue(nthFuture > 0, "nthFuture must be greater than 0.");
    if (nthFuture <= 6) { // We look for expiries in the first 6 serial months after curveDate
      final LocalDate expiry = getIRFutureMonthlyExpiry(nthFuture, valDate);
      final LocalDate previousMonday = expiry.minusDays(2);
      return previousMonday;
    }   // And for Quarterly expiries thereafter
    final int nthExpiryAfterSixMonths = nthFuture - 6;
    final LocalDate sixMonthsForward = valDate.plusMonths(6);
    return getApproximateIRFutureQuarterlyExpiry(nthExpiryAfterSixMonths, sixMonthsForward);
  }

  public static LocalDate getIRFutureMonthlyExpiry(final int nthMonth, final LocalDate valDate) {
    Validate.isTrue(nthMonth > 0, "nthFuture must be greater than 0.");
    LocalDate expiry = valDate.with(THIRD_WED_ADJUSTER); // Compute the 3rd Wednesday of valuationDate's month
    if (!expiry.isAfter(valDate)) { // If it is not strictly after valuationDate...
      expiry = (valDate.plusMonths(1)).with(THIRD_WED_ADJUSTER);  // nextExpiry is third Wednesday of next month
    }
    if (nthMonth > 1) {
      expiry = (expiry.plusMonths(nthMonth - 1)).with(THIRD_WED_ADJUSTER);
    }
    return expiry;
  }

  public static LocalDate getIRFutureQuarterlyExpiryDate(final int nthFuture, final LocalDate valDate, final Calendar holidayCalendar) {
    return IMMFutureAndFutureOptionQuarterlyExpiryCalculator.getInstance().getExpiryDate(nthFuture, valDate, holidayCalendar);
  }

  public static LocalDate getApproximateIRFutureQuarterlyExpiry(final int nthFuture, final LocalDate valDate) {
    return IMMFutureAndFutureOptionQuarterlyExpiryCalculator.getInstance().getExpiryMonth(nthFuture, valDate);
  }

}
