/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.math.rootfinding;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

import com.opengamma.financial.interestrate.InterestRateCalculator;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.SingleCurveFinder;
import com.opengamma.financial.interestrate.SingleCurveJacobian;
import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.financial.interestrate.fra.definition.ForwardRateAgreement;
import com.opengamma.financial.interestrate.libor.Libor;
import com.opengamma.financial.interestrate.swap.definition.Swap;
import com.opengamma.financial.model.interestrate.curve.InterpolatedYieldCurve;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.interpolation.CubicSplineInterpolatorWithSensitivities1D;
import com.opengamma.math.interpolation.Extrapolator1D;
import com.opengamma.math.interpolation.ExtrapolatorMethod;
import com.opengamma.math.interpolation.FlatExtrapolator;
import com.opengamma.math.interpolation.FlatExtrapolatorWithSensitivities;
import com.opengamma.math.interpolation.InterpolationResult;
import com.opengamma.math.interpolation.InterpolationResultWithSensitivities;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.interpolation.Interpolator1DCubicSplineDataBundle;
import com.opengamma.math.interpolation.Interpolator1DCubicSplineWithSensitivitiesDataBundle;
import com.opengamma.math.interpolation.Interpolator1DDataBundle;
import com.opengamma.math.interpolation.Interpolator1DWithSensitivities;
import com.opengamma.math.interpolation.LinearExtrapolator;
import com.opengamma.math.interpolation.LinearExtrapolatorWithSensitivity;
import com.opengamma.math.interpolation.NaturalCubicSplineInterpolator1D;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.matrix.DoubleMatrix2D;
import com.opengamma.math.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.math.rootfinding.newton.FiniteDifferenceJacobianCalculator;
import com.opengamma.math.rootfinding.newton.JacobianCalculator;
import com.opengamma.math.rootfinding.newton.NewtonDefaultVectorRootFinder;
import com.opengamma.math.rootfinding.newton.ShermanMorrisonVectorRootFinder;
import com.opengamma.util.monitor.OperationTimer;

/**
 * 
 */
public class MultiInstrumentSingleCurveBootStrapTest {

  private static final Logger s_logger = LoggerFactory.getLogger(YieldCurveBootStrapTest.class);
  private static final int HOTSPOT_WARMUP_CYCLES = 0;
  private static final int BENCHMARK_CYCLES = 1;
  private static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister64.DEFAULT_SEED);

  private static final Interpolator1D<Interpolator1DCubicSplineDataBundle, InterpolationResult> EXTRAPOLATOR;
  private static final Interpolator1D<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities> EXTRAPOLATOR_WITH_SENSITIVITY;
  private static final List<InterestRateDerivative> INSTRUMENTS;
  private static final double[] MARKET_VALUES;
  private static final YieldAndDiscountCurve CURVE;

  private static final double[] NODE_TIMES;

  private static final double EPS = 1e-8;
  private static final int STEPS = 100;
  private static final DoubleMatrix1D X0;

  private static final InterestRateCalculator SWAP_RATE_CALCULATOR = new InterestRateCalculator();
  private static final Function1D<DoubleMatrix1D, DoubleMatrix1D> SINGLE_CURVE_FINDER;
  private static final JacobianCalculator SINGLE_CURVE_JACOBIAN;

  private static final Function1D<Double, Double> DUMMY_CURVE = new Function1D<Double, Double>() {

    // private static final double A = -0.0325;
    // private static final double B = 0.021;
    // private static final double C = 0.52;
    // private static final double D = 0.055;

    private static final double A = 0;
    private static final double B = 0.004148649;
    private static final double C = 0.056397936;
    private static final double D = 0.004457019;
    private static final double E = 0.000429628;

    @Override
    public Double evaluate(final Double x) {
      return (A + B * x) * Math.exp(-C * x) + E * x + D;
    }
  };

  static {

    INSTRUMENTS = new ArrayList<InterestRateDerivative>();

    // final double[] liborMaturities = new double[] {};// {3. / 12.}; // note using 1m and 2m LIBOR tenors for what should be the 3m-libor curve is probably wrong
    // final double[] fraMaturities = new double[] {0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0};
    // final double[] cashMaturities = new double[] {};// {1 / 365.25, 7 / 365.25, 1.0 / 12.0};
    // final int[] swapSemiannualGrid = new int[] {};// 4, 6, 8, 10, 12, 14, 16, 18, 20, 30, 40, 50, 60};
    //    
    final double[] liborMaturities = new double[] {0.013888889, 0.033333333, 0.052777778, 0.088888889, 0.166666667, 0.261111111, 0.336111111, 0.416666667, 0.508333333, 0.583333333, 0.666666667,
        0.761111111, 0.838888889, 0.916666667, 1.011111111}; // 
    final double[] fraMaturities = new double[] {1.491666667, 1.744444444, 1.997222222};
    final double[] cashMaturities = new double[] {};
    final double[] swapMaturities = new double[] {2.005555556, 3.002777778, 4, 5, 7.008333333, 10, 15, 20.00277778, 25.00555556, 30.00555556, 35.00833333, 50.01388889};

    // final int nNodes = liborMaturities.length + fraMaturities.length + cashMaturities.length + swapSemiannualGrid.length;
    final int nNodes = liborMaturities.length + fraMaturities.length + cashMaturities.length + swapMaturities.length;

    NODE_TIMES = new double[nNodes];
    int index = 0;

    InterestRateDerivative ird;

    for (final double t : liborMaturities) {
      ird = new Libor(t);
      INSTRUMENTS.add(ird);
      NODE_TIMES[index++] = t;
    }
    for (final double t : fraMaturities) {
      ird = new ForwardRateAgreement(t - 0.25, t);
      INSTRUMENTS.add(ird);
      NODE_TIMES[index++] = t;
    }

    for (final double t : cashMaturities) {
      ird = new Cash(t);
      INSTRUMENTS.add(ird);
      NODE_TIMES[index++] = t;
    }

    // for (final int element : swapSemiannualGrid) {
    // final Swap swap = setupSwap(element);
    // INSTRUMENTS.add(swap);
    //
    // final double t = swap.getFloatingPaymentTimes()[swap.getNumberOfFloatingPayments() - 1] + Math.max(0.0, swap.getDeltaEnd()[swap.getNumberOfFloatingPayments() - 1]);
    // NODE_TIMES[index++] = t;
    // }

    for (final double t : swapMaturities) {
      final Swap swap = setupSwap(t);
      INSTRUMENTS.add(swap);
      NODE_TIMES[index++] = t;
    }

    if (INSTRUMENTS.size() != (nNodes)) {
      throw new IllegalArgumentException("number of instruments not equal to number of nodes");
    }

    Arrays.sort(NODE_TIMES);

    final Interpolator1D<Interpolator1DCubicSplineDataBundle, InterpolationResult> cubicInterpolator = new NaturalCubicSplineInterpolator1D();
    final Interpolator1DWithSensitivities<Interpolator1DCubicSplineWithSensitivitiesDataBundle> cubicInterpolatorWithSense = new CubicSplineInterpolatorWithSensitivities1D();
    final ExtrapolatorMethod<Interpolator1DCubicSplineDataBundle, InterpolationResult> linear_em = new LinearExtrapolator<Interpolator1DCubicSplineDataBundle, InterpolationResult>();
    final ExtrapolatorMethod<Interpolator1DCubicSplineDataBundle, InterpolationResult> flat_em = new FlatExtrapolator<Interpolator1DCubicSplineDataBundle, InterpolationResult>();
    final ExtrapolatorMethod<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities> linear_em_sense = new LinearExtrapolatorWithSensitivity<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities>();
    final ExtrapolatorMethod<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities> flat_em_sense = new FlatExtrapolatorWithSensitivities<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities>();
    EXTRAPOLATOR = new Extrapolator1D<Interpolator1DCubicSplineDataBundle, InterpolationResult>(linear_em, flat_em, cubicInterpolator);
    EXTRAPOLATOR_WITH_SENSITIVITY = new Extrapolator1D<Interpolator1DCubicSplineWithSensitivitiesDataBundle, InterpolationResultWithSensitivities>(linear_em_sense, flat_em_sense,
        cubicInterpolatorWithSense);

    // set up curve to obtain "market" prices
    final double[] yields = new double[nNodes];

    for (int i = 0; i < nNodes; i++) {
      yields[i] = DUMMY_CURVE.evaluate(NODE_TIMES[i]);
    }

    CURVE = makeYieldCurve(yields, NODE_TIMES, EXTRAPOLATOR);

    // now get market prices
    MARKET_VALUES = new double[nNodes];
    final double[] rates = new double[nNodes];

    for (int i = 0; i < nNodes; i++) {
      MARKET_VALUES[i] = SWAP_RATE_CALCULATOR.getRate(CURVE, CURVE, INSTRUMENTS.get(i));
      rates[i] = 0.05;
    }
    X0 = new DoubleMatrix1D(rates);

    SINGLE_CURVE_FINDER = new SingleCurveFinder(INSTRUMENTS, MARKET_VALUES, NODE_TIMES, EXTRAPOLATOR);
    SINGLE_CURVE_JACOBIAN = new SingleCurveJacobian<Interpolator1DCubicSplineWithSensitivitiesDataBundle>(INSTRUMENTS, NODE_TIMES, EXTRAPOLATOR_WITH_SENSITIVITY);

  }

  @Test
  public void testNewton() {
    VectorRootFinder rootFinder = new NewtonDefaultVectorRootFinder(EPS, EPS, STEPS, SINGLE_CURVE_JACOBIAN);
    doHotSpot(rootFinder, "default Newton, single curve", SINGLE_CURVE_FINDER);
  }

  @Test
  public void testBroyden() {
    VectorRootFinder rootFinder = new BroydenVectorRootFinder(EPS, EPS, STEPS, SINGLE_CURVE_JACOBIAN);
    doHotSpot(rootFinder, "Broyden, single curve", SINGLE_CURVE_FINDER);
  }

  @Test
  public void testShermanMorrison() {
    VectorRootFinder rootFinder = new ShermanMorrisonVectorRootFinder(EPS, EPS, STEPS, SINGLE_CURVE_JACOBIAN);
    doHotSpot(rootFinder, "Broyden, single curve", SINGLE_CURVE_FINDER);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSingleJacobian() {
    final JacobianCalculator jacobianFD = new FiniteDifferenceJacobianCalculator(1e-8);
    final DoubleMatrix2D jacExact = SINGLE_CURVE_JACOBIAN.evaluate(X0, SINGLE_CURVE_FINDER);
    final DoubleMatrix2D jacFD = jacobianFD.evaluate(X0, SINGLE_CURVE_FINDER);
    // System.out.println("exact: " + jacExact.toString());
    // System.out.println("FD: " + jacFD.toString());
    assertMatrixEquals(jacExact, jacFD, 1e-7);
  }

  private void doHotSpot(final VectorRootFinder rootFinder, final String name, final Function1D<DoubleMatrix1D, DoubleMatrix1D> functor) {
    for (int i = 0; i < HOTSPOT_WARMUP_CYCLES; i++) {
      doTest(rootFinder, (SingleCurveFinder) functor);
    }
    if (BENCHMARK_CYCLES > 0) {
      final OperationTimer timer = new OperationTimer(s_logger, "processing {} cycles on " + name, BENCHMARK_CYCLES);
      for (int i = 0; i < BENCHMARK_CYCLES; i++) {
        doTest(rootFinder, (SingleCurveFinder) functor);
      }
      timer.finished();
    }
  }

  private void doTest(final VectorRootFinder rootFinder, final SingleCurveFinder functor) {
    final DoubleMatrix1D yieldCurveNodes = rootFinder.getRoot(functor, X0);
    final YieldAndDiscountCurve curve = makeYieldCurve(yieldCurveNodes.getData(), NODE_TIMES, EXTRAPOLATOR);
    // System.out.println("times: " + (new DoubleMatrix1D(NODE_TIMES)).toString());
    // System.out.println("market rates: " + (new DoubleMatrix1D(MARKET_VALUES)).toString());
    // System.out.println("yields: " + yieldCurveNodes.toString());
    for (int i = 0; i < MARKET_VALUES.length; i++) {
      assertEquals(MARKET_VALUES[i], SWAP_RATE_CALCULATOR.getRate(curve, curve, INSTRUMENTS.get(i)), EPS);
    }
  }

  private static YieldAndDiscountCurve makeYieldCurve(final double[] yields, final double[] times, final Interpolator1D<? extends Interpolator1DDataBundle, ? extends InterpolationResult> interpolator) {
    return new InterpolatedYieldCurve(times, yields, interpolator);
  }

  private static Swap setupSwap(final double time) {
    int index = (int) Math.round(2 * time);
    return setupSwap(index);
  }

  private static Swap setupSwap(final int payments) {
    final double[] fixed = new double[payments];
    final double[] floating = new double[2 * payments];
    final double[] deltaStart = new double[2 * payments];
    final double[] deltaEnd = new double[2 * payments];
    final double sigma = 0.0 / 365.0;
    for (int i = 0; i < payments; i++) {
      fixed[i] = 0.5 * (1 + i) + sigma * (RANDOM.nextDouble() - 0.5);
      floating[2 * i + 1] = fixed[i];
    }
    for (int i = 0; i < 2 * payments; i++) {
      if (i % 2 == 0) {
        floating[i] = 0.25 * (1 + i) + sigma * (RANDOM.nextDouble() - 0.5);
      }
      deltaStart[i] = sigma * (i == 0 ? RANDOM.nextDouble() : (RANDOM.nextDouble() - 0.5));
      deltaEnd[i] = sigma * (RANDOM.nextDouble() - 0.5);
    }
    return new Swap(fixed, floating, deltaStart, deltaEnd);
  }

  private void assertMatrixEquals(final DoubleMatrix2D m1, final DoubleMatrix2D m2, final double eps) {
    final int m = m1.getNumberOfRows();
    final int n = m1.getNumberOfColumns();
    assertEquals(m2.getNumberOfRows(), m);
    assertEquals(m2.getNumberOfColumns(), n);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        assertEquals(m1.getEntry(i, j), m2.getEntry(i, j), eps);
      }
    }
  }
}
