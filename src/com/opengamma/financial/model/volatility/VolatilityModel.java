/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.volatility;

/**
 * 
 * @param <T> The type of the abscissa(s) 
 */

public interface VolatilityModel<T> {

  Double getVolatility(T t);
}
