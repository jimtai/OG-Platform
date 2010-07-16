/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.model.interestrate.curve.InterpolatedYieldCurve;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.interpolation.InterpolationResult;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.interpolation.Interpolator1DDataBundle;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.util.ArgumentChecker;

/**
 * 
 */
public class DoubleCurveFinder extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {
  private final List<InterestRateDerivative> _derivatives;
  private final double[] _marketRates;
  private final double[] _forwardTimeGrid;
  private final double[] _fundingTimeGrid;
  private YieldAndDiscountCurve _fwdCurve;
  private YieldAndDiscountCurve _fundCurve;
  private final Interpolator1D<? extends Interpolator1DDataBundle, InterpolationResult> _forwardInterpolator;
  private final Interpolator1D<? extends Interpolator1DDataBundle, InterpolationResult> _fundingInterpolator;
  private final int _nInterestRateDerivatives, _nFwdNodes, _nFundNodes;
  private final InterestRateCalculator _swapRateCalculator = new InterestRateCalculator();

  public DoubleCurveFinder(final List<InterestRateDerivative> derivatives, final double[] marketRates, final double[] forwardTimeGrid, final double[] fundingTimeGrid,
      final YieldAndDiscountCurve forwardCurve, final YieldAndDiscountCurve fundCurve, final Interpolator1D<? extends Interpolator1DDataBundle, InterpolationResult> forwardInterpolator,
      final Interpolator1D<? extends Interpolator1DDataBundle, InterpolationResult> fundingInterpolator) {
    Validate.notNull(derivatives);
    Validate.notNull(marketRates);
    if (forwardTimeGrid != null) {
      Validate.notNull(forwardInterpolator);
      ArgumentChecker.notEmpty(forwardTimeGrid, "forward time grid");
    } else {
      Validate.notNull(forwardCurve); // if you are not fitting this curve it must be supplied
    }
    if (fundingTimeGrid != null) {
      Validate.notNull(fundingInterpolator);
      ArgumentChecker.notEmpty(fundingTimeGrid, "funding time grid");
    } else {
      Validate.notNull(fundCurve);
    }
    Validate.notEmpty(derivatives);
    if (derivatives.size() != marketRates.length) {
      throw new IllegalArgumentException("Must be same number of market data points as derivatives");
    }
    _derivatives = derivatives;
    _marketRates = marketRates;
    _forwardTimeGrid = forwardTimeGrid;
    _fundingTimeGrid = fundingTimeGrid;
    _forwardInterpolator = forwardInterpolator;
    _fundingInterpolator = fundingInterpolator;
    _nInterestRateDerivatives = _derivatives.size();
    _nFwdNodes = (forwardTimeGrid == null ? 0 : forwardTimeGrid.length);
    _nFundNodes = (fundingTimeGrid == null ? 0 : fundingTimeGrid.length);
    _fwdCurve = forwardCurve;
    _fundCurve = fundCurve;
    if (_nInterestRateDerivatives != _nFwdNodes + _nFundNodes) {
      throw new IllegalArgumentException("total number of nodes does not match number of instruments");
    }
  }

  // TODO this needs to be split into 4 separate functions for the various combinations of null / not null inputs
  @Override
  public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
    Validate.notNull(x);
    if (x.getNumberOfElements() != (_nFwdNodes + _nFundNodes)) {
      throw new IllegalArgumentException("fitting vector not same length as number of nodes");
    }

    if (_nFwdNodes > 0) {
      double[] yields = Arrays.copyOfRange(x.getData(), 0, _nFwdNodes);
      _fwdCurve = new InterpolatedYieldCurve(_forwardTimeGrid, yields, _forwardInterpolator);
    }

    if (_nFundNodes > 0) {
      double[] yields = Arrays.copyOfRange(x.getData(), _nFwdNodes, x.getNumberOfElements());
      _fundCurve = new InterpolatedYieldCurve(_fundingTimeGrid, yields, _fundingInterpolator);
    }

    final double[] res = new double[_nInterestRateDerivatives];
    for (int i = 0; i < _nInterestRateDerivatives; i++) {
      res[i] = _swapRateCalculator.getRate(_fwdCurve, _fundCurve, _derivatives.get(i)) - _marketRates[i];
    }
    return new DoubleMatrix1D(res);
  }
}
