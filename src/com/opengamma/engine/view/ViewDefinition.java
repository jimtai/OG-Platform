/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.view;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.livedata.msg.UserPrincipal;
import com.opengamma.util.ArgumentChecker;

/**
 * The encapsulated logic that controls how precisely a view is to be constructed
 * and computed.
 */
public class ViewDefinition implements Serializable {
  private final String _name;
  private final UniqueIdentifier _portfolioId;
  private final UserPrincipal _user;
  private Long _minimumRecalculationPeriod;
  private final Map<String, ViewCalculationConfiguration> _calculationConfigurationsByName =
    new TreeMap<String, ViewCalculationConfiguration>();
  
  public ViewDefinition(String name, UniqueIdentifier portfolioId, String userName) {
    ArgumentChecker.notNull(name, "View name");
    ArgumentChecker.notNull(portfolioId, "Portfolio id");
    ArgumentChecker.notNull(userName, "User name");
    
    _name = name;
    _portfolioId = portfolioId;
    
    try {
      _user = new UserPrincipal(userName, InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException e) {
      throw new OpenGammaRuntimeException("Could not obtain local host address", e);
    }
  }
  
  public ViewDefinition(String name, UniqueIdentifier portfolioId, UserPrincipal user) {
    ArgumentChecker.notNull(name, "View name");
    ArgumentChecker.notNull(portfolioId, "Portfolio id");
    ArgumentChecker.notNull(user, "User name");
    
    _name = name;
    _portfolioId = portfolioId;
    _user = user;
  }
  
  public Set<String> getAllValueRequirements() {
    Set<String> requirements = new TreeSet<String>();
    for(ViewCalculationConfiguration calcConfig : _calculationConfigurationsByName.values()) {
      requirements.addAll(calcConfig.getAllValueRequirements());
    }
    return requirements;
  }

  public String getName() {
    return _name;
  }

  public UniqueIdentifier getPortfolioId() {
    return _portfolioId;
  }
  
  public UserPrincipal getUser() {
    return _user;
  }
  public Collection<ViewCalculationConfiguration> getAllCalculationConfigurations() {
    return new ArrayList<ViewCalculationConfiguration>(_calculationConfigurationsByName.values());
  }
  
  public Set<String> getAllCalculationConfigurationNames() {
    return Collections.unmodifiableSet(_calculationConfigurationsByName.keySet());
  }
  
  public Map<String, ViewCalculationConfiguration> getAllCalculationConfigurationsByName() {
    return Collections.unmodifiableMap(_calculationConfigurationsByName);
  }
  
  public ViewCalculationConfiguration getCalculationConfiguration(String configurationName) {
    return _calculationConfigurationsByName.get(configurationName);
  }
  
  public void addViewCalculationConfiguration(ViewCalculationConfiguration calcConfig) {
    ArgumentChecker.notNull(calcConfig, "calculation configuration");
    ArgumentChecker.notNull(calcConfig.getName(), "Configuration name");
    _calculationConfigurationsByName.put(calcConfig.getName(), calcConfig);
  }
  
  public void addValueDefinition(String calculationConfigurationName, String securityType, String requirementName) {
    ViewCalculationConfiguration calcConfig = _calculationConfigurationsByName.get(calculationConfigurationName);
    if(calcConfig == null) {
      calcConfig = new ViewCalculationConfiguration(this, calculationConfigurationName);
      _calculationConfigurationsByName.put(calculationConfigurationName, calcConfig);
    }
    calcConfig.addValueRequirement(securityType, requirementName);
  }

  /**
   * @return the minimumRecalculationPeriod
   */
  public Long getMinimumRecalculationPeriod() {
    return _minimumRecalculationPeriod;
  }

  /**
   * @param minimumRecalculationPeriod the minimumRecalculationPeriod to set
   */
  public void setMinimumRecalculationPeriod(Long minimumRecalculationPeriod) {
    _minimumRecalculationPeriod = minimumRecalculationPeriod;
  }

}
