/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.credit.creditdefaultswap.pricing.legacy;

import javax.time.calendar.ZonedDateTime;

import com.opengamma.analytics.financial.credit.PriceType;
import com.opengamma.analytics.financial.credit.calibratehazardratecurve.HazardRateCurve;
import com.opengamma.analytics.financial.credit.cds.ISDACurve;
import com.opengamma.analytics.financial.credit.creditdefaultswap.definition.legacy.LegacyVanillaCreditDefaultSwapDefinition;
import com.opengamma.util.ArgumentChecker;

/**
 * Class containing methods for the valuation of a Legacy vanilla CDS
 */
public class PresentValueLegacyVanillaCreditDefaultSwap {

  //----------------------------------------------------------------------------------------------------------------------------------------

  // NOTE : For most types of legacy CDS, the same underlying pricing model will be used
  // NOTE : However, if required we can override these methods to provide bespoke pricing models
  // NOTE : for specific types of CDS if required

  //----------------------------------------------------------------------------------------------------------------------------------------

  // Create a PV calculator for a legacy CDS object
  private static final PresentValueLegacyCreditDefaultSwap presentValueLegacyCreditDefaultSwap = new PresentValueLegacyCreditDefaultSwap();

  // ----------------------------------------------------------------------------------------------------------------------------------------

  // Public method for computing the PV of a CDS based on an input CDS contract (with a hazard rate curve calibrated to market observed data)

  public double getPresentValueLegacyVanillaCreditDefaultSwap(
      final ZonedDateTime valuationDate,
      final LegacyVanillaCreditDefaultSwapDefinition vanillaCDS,
      final ISDACurve yieldCurve,
      final HazardRateCurve hazardRateCurve,
      final PriceType priceType) {

    // ----------------------------------------------------------------------------------------------------------------------------------------

    // Check input objects are not null

    ArgumentChecker.notNull(valuationDate, "Valuation date");
    ArgumentChecker.notNull(vanillaCDS, "LegacyVanillaCreditDefaultSwapDefinition");
    ArgumentChecker.notNull(yieldCurve, "YieldCurve");
    ArgumentChecker.notNull(hazardRateCurve, "HazardRateCurve");
    ArgumentChecker.notNull(priceType, "price type");

    // ----------------------------------------------------------------------------------------------------------------------------------------

    // Calculate the CDS PV
    final double vanillaCDSPresentValue = presentValueLegacyCreditDefaultSwap.getPresentValueLegacyCreditDefaultSwap(valuationDate, vanillaCDS, yieldCurve, hazardRateCurve, priceType);

    // ----------------------------------------------------------------------------------------------------------------------------------------

    return vanillaCDSPresentValue;
  }

  // ----------------------------------------------------------------------------------------------------------------------------------------

  // Public method to calculate the par spread of a CDS at contract inception (with a hazard rate curve calibrated to market observed data)

  public double getParSpreadLegacyVanillaCreditDefaultSwap(
      final ZonedDateTime valuationDate,
      final LegacyVanillaCreditDefaultSwapDefinition vanillaCDS,
      final ISDACurve yieldCurve,
      final HazardRateCurve hazardRateCurve,
      final PriceType priceType) {

    // ----------------------------------------------------------------------------------------------------------------------------------------

    // Check input objects are not null

    ArgumentChecker.notNull(valuationDate, "Valuation date");
    ArgumentChecker.notNull(vanillaCDS, "LegacyVanillaCreditDefaultSwapDefinition");
    ArgumentChecker.notNull(yieldCurve, "YieldCurve");
    ArgumentChecker.notNull(hazardRateCurve, "HazardRateCurve");
    ArgumentChecker.notNull(priceType, "price type");

    // ----------------------------------------------------------------------------------------------------------------------------------------

    // Calculate the CDS par spread
    final double vanillaCDSParSpread = presentValueLegacyCreditDefaultSwap.getParSpreadLegacyCreditDefaultSwap(valuationDate, vanillaCDS, yieldCurve, hazardRateCurve, priceType);

    // ----------------------------------------------------------------------------------------------------------------------------------------

    return vanillaCDSParSpread;
  }

  // ----------------------------------------------------------------------------------------------------------------------------------------
}
