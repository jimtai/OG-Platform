/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.view.calcnode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetResolver;
import com.opengamma.engine.function.CompiledFunctionRepository;
import com.opengamma.engine.function.CompiledFunctionService;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.function.FunctionInputsImpl;
import com.opengamma.engine.function.FunctionInvoker;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.ViewProcessor;
import com.opengamma.engine.view.cache.CacheSelectHint;
import com.opengamma.engine.view.cache.DelayedViewComputationCache;
import com.opengamma.engine.view.cache.FilteredViewComputationCache;
import com.opengamma.engine.view.cache.NonDelayedViewComputationCache;
import com.opengamma.engine.view.cache.NotCalculatedSentinel;
import com.opengamma.engine.view.cache.ViewComputationCache;
import com.opengamma.engine.view.cache.ViewComputationCacheSource;
import com.opengamma.engine.view.cache.WriteBehindViewComputationCache;
import com.opengamma.engine.view.calcnode.stats.FunctionInvocationStatisticsGatherer;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.monitor.InvocationCount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.Pair;

/**
 * A calculation node implementation. The node can only be used by one thread - i.e. executeJob cannot be called concurrently to do multiple jobs. To execute multiple jobs concurrently separate
 * calculation nodes must be used.
 * <p>
 * The function repository (and anything else) must be properly initialized and ready for use by the node when it receives its first job. Responsibility for initialization should therefore lie with
 * whatever will logically be dispatching jobs. This is typically a {@link ViewProcessor} for local nodes or a {@link RemoteNodeClient} for remote nodes.
 */
public abstract class AbstractCalculationNode implements CalculationNode {
  private static final Logger s_logger = LoggerFactory.getLogger(AbstractCalculationNode.class);
  private final ViewComputationCacheSource _cacheSource;
  private final CompiledFunctionService _functionCompilationService;
  private final FunctionExecutionContext _functionExecutionContext;
  private final ComputationTargetResolver _targetResolver;
  private final ViewProcessorQuerySender _viewProcessorQuerySender;
  private final FunctionInvocationStatisticsGatherer _functionInvocationStatistics;
  private String _nodeId;
  private final ExecutorService _writeBehindExecutorService;

  protected AbstractCalculationNode(ViewComputationCacheSource cacheSource, CompiledFunctionService functionCompilationService,
      FunctionExecutionContext functionExecutionContext, ComputationTargetResolver targetResolver, ViewProcessorQuerySender calcNodeQuerySender, String nodeId,
      final ExecutorService writeBehindExecutorService, FunctionInvocationStatisticsGatherer functionInvocationStatistics) {
    ArgumentChecker.notNull(cacheSource, "Cache Source");
    ArgumentChecker.notNull(functionCompilationService, "Function compilation service");
    ArgumentChecker.notNull(functionExecutionContext, "Function Execution Context");
    ArgumentChecker.notNull(targetResolver, "Target Resolver");
    ArgumentChecker.notNull(calcNodeQuerySender, "Calc Node Query Sender");
    ArgumentChecker.notNull(nodeId, "Calculation node ID");
    ArgumentChecker.notNull(functionInvocationStatistics, "function invocation statistics");

    _cacheSource = cacheSource;
    _functionCompilationService = functionCompilationService;
    // Take a copy of the execution context as we will modify it during execution which isn't good if there are other CalcNodes in our JVM
    _functionExecutionContext = functionExecutionContext.clone();
    _targetResolver = targetResolver;
    _viewProcessorQuerySender = calcNodeQuerySender;
    _nodeId = nodeId;
    _writeBehindExecutorService = writeBehindExecutorService;
    _functionInvocationStatistics = functionInvocationStatistics;
  }

  public ViewComputationCacheSource getCacheSource() {
    return _cacheSource;
  }

  public CompiledFunctionService getFunctionCompilationService() {
    return _functionCompilationService;
  }

  public FunctionExecutionContext getFunctionExecutionContext() {
    return _functionExecutionContext;
  }

  public ComputationTargetResolver getTargetResolver() {
    return _targetResolver;
  }

  protected ViewProcessorQuerySender getViewProcessorQuerySender() {
    return _viewProcessorQuerySender;
  }

  protected ExecutorService getWriteBehindExecutorService() {
    return _writeBehindExecutorService;
  }

  protected FunctionInvocationStatisticsGatherer getFunctionInvocationStatistics() {
    return _functionInvocationStatistics;
  }

  @Override
  public String getNodeId() {
    return _nodeId;
  }

  public void setNodeId(final String nodeId) {
    ArgumentChecker.notNull(nodeId, "nodeId");
    _nodeId = nodeId;
  }

  private void postNotCalculated(final Collection<ValueSpecification> outputs, final FilteredViewComputationCache cache) {
    final Collection<ComputedValue> results = new ArrayList<ComputedValue>(outputs.size());
    for (ValueSpecification output : outputs) {
      results.add(new ComputedValue(output, NotCalculatedSentinel.getInstance()));
    }
    cache.putValues(results);
  }

  protected List<CalculationJobResultItem> executeJobItems(final CalculationJob job, final DelayedViewComputationCache cache,
      final CompiledFunctionRepository functions, final String calculationConfiguration) {
    final List<CalculationJobResultItem> resultItems = new ArrayList<CalculationJobResultItem>();
    for (CalculationJobItem jobItem : job.getJobItems()) {
      if (job.isCancelled()) {
        return null;
      }
      CalculationJobResultItem resultItem;
      try {
        invoke(functions, jobItem, cache, new DeferredInvocationStatistics(getFunctionInvocationStatistics(), calculationConfiguration));
        resultItem = new CalculationJobResultItem(jobItem);
      } catch (MissingInputException e) {
        // NOTE kirk 2009-10-20 -- We intentionally only do the message here so that we don't
        // litter the logs with stack traces; the inputs missing have also already been
        // written at INFO level
        s_logger.info("Unable to invoke {} due to missing inputs", jobItem);
        postNotCalculated(jobItem.getOutputs(), cache);
        resultItem = new CalculationJobResultItem(jobItem, e);
      } catch (Throwable t) {
        //s_logger.error("Invoking {} threw exception: {}", jobItem.getFunctionUniqueIdentifier(), t.getMessage());
        s_logger.info("Caught", t);
        postNotCalculated(jobItem.getOutputs(), cache);
        resultItem = new CalculationJobResultItem(jobItem, t);
      }
      resultItems.add(resultItem);
    }
    return resultItems;
  }

  private static final InvocationCount s_executeJob = new InvocationCount("executeJob");
  private static final InvocationCount s_executeJobItems = new InvocationCount("executeJobItems");
  private static final InvocationCount s_writeBack = new InvocationCount("writeBack");

  public CalculationJobResult executeJob(final CalculationJob job) {
    s_logger.info("Executing {} on {}", job, _nodeId);
    s_executeJob.enter();
    final CalculationJobSpecification spec = job.getSpecification();
    getFunctionExecutionContext().setViewProcessorQuery(new ViewProcessorQuery(getViewProcessorQuerySender(), spec));
    getFunctionExecutionContext().setValuationTime(spec.getValuationTime());
    getFunctionExecutionContext().setValuationClock(DateUtils.fixedClockUTC(spec.getValuationTime()));
    final CompiledFunctionRepository functions = getFunctionCompilationService().compileFunctionRepository(spec.getValuationTime());
    final DelayedViewComputationCache cache = getDelayedViewComputationCache(getCache(spec), job.getCacheSelectHint());
    long executionTime = System.nanoTime();
    final String calculationConfiguration = spec.getCalcConfigName();
    s_executeJobItems.enter();
    final List<CalculationJobResultItem> resultItems = executeJobItems(job, cache, functions, calculationConfiguration);
    if (resultItems == null) {
      return null;
    }
    s_executeJobItems.leave();
    s_writeBack.enter();
    cache.waitForPendingWrites();
    s_writeBack.leave();
    executionTime = System.nanoTime() - executionTime;
    CalculationJobResult jobResult = new CalculationJobResult(spec, executionTime, resultItems, getNodeId());
    s_executeJob.leave();
    s_logger.info("Executed {} in {}ns", job, executionTime);
    return jobResult;
  }

  private DelayedViewComputationCache getDelayedViewComputationCache(ViewComputationCache cache,
      CacheSelectHint cacheSelectHint) {
    if (getWriteBehindExecutorService() == null) {
      return new NonDelayedViewComputationCache(cache, cacheSelectHint);
    } else {
      return new WriteBehindViewComputationCache(cache, cacheSelectHint, getWriteBehindExecutorService());
    }
  }

  @Override
  public ViewComputationCache getCache(CalculationJobSpecification spec) {
    ViewComputationCache cache = getCacheSource().getCache(spec.getViewCycleId(), spec.getCalcConfigName());
    return cache;
  }

  private static Set<ValueRequirement> plat2290(final Set<ValueSpecification> outputs) {
    final Set<ValueRequirement> result = Sets.newHashSetWithExpectedSize(outputs.size());
    for (ValueSpecification output : outputs) {
      result.add(output.toRequirementSpecification());
    }
    return result;
  }

  private static final InvocationCount s_resolveTarget = new InvocationCount("resolveTarget");
  private static final InvocationCount s_prepareInputs = new InvocationCount("prepareInputs");

  private void invoke(final CompiledFunctionRepository functions, final CalculationJobItem jobItem, final DelayedViewComputationCache cache, final DeferredInvocationStatistics statistics) {
    // TODO: can we do the target resolution and parameter resolution in parallel ?
    // TODO: can we do the target resolution in advance ?
    final String functionUniqueId = jobItem.getFunctionUniqueIdentifier();
    s_resolveTarget.enter();
    final ComputationTarget target = getTargetResolver().resolve(jobItem.getComputationTargetSpecification());
    s_resolveTarget.leave();
    if (target == null) {
      throw new OpenGammaRuntimeException("Unable to resolve specification " + jobItem.getComputationTargetSpecification());
    }
    s_logger.debug("Invoking {} on target {}", functionUniqueId, target);
    final FunctionInvoker invoker = functions.getInvoker(functionUniqueId);
    if (invoker == null) {
      throw new NullPointerException("Unable to locate " + functionUniqueId + " in function repository.");
    }
    // set parameters
    getFunctionExecutionContext().setFunctionParameters(jobItem.getFunctionParameters());
    // assemble inputs
    s_prepareInputs.enter();
    final Collection<ComputedValue> inputs = new HashSet<ComputedValue>();
    final Collection<ValueSpecification> missing = new HashSet<ValueSpecification>();
    int inputBytes = 0;
    int inputSamples = 0;
    for (Pair<ValueSpecification, Object> input : cache.getValues(jobItem.getInputs())) {
      if ((input.getValue() == null) || (input.getValue() instanceof MissingInput)) {
        missing.add(input.getKey());
      } else {
        final ComputedValue value = new ComputedValue(input.getKey(), input.getValue());
        inputs.add(value);
        final Integer bytes = cache.estimateValueSize(value);
        if (bytes != null) {
          inputBytes += bytes;
          inputSamples++;
        }
      }
    }
    statistics.setDataInputBytes(inputBytes, inputSamples);
    s_prepareInputs.leave();
    if (!missing.isEmpty()) {
      if (invoker.canHandleMissingInputs()) {
        s_logger.debug("Executing even with missing inputs {}", missing);
      } else {
        s_logger.info("Not able to execute as missing inputs {}", missing);
        throw new MissingInputException(missing, functionUniqueId);
      }
    }
    final FunctionInputs functionInputs = new FunctionInputsImpl(inputs, missing);
    // execute
    statistics.beginInvocation();
    final Set<ValueSpecification> outputs = jobItem.getOutputs();
    Collection<ComputedValue> results = invoker.execute(getFunctionExecutionContext(), functionInputs, target, plat2290(outputs));
    if (results == null) {
      throw new NullPointerException("No results returned by invoker " + invoker);
    }
    statistics.endInvocation();
    statistics.setFunctionIdentifier(functionUniqueId);
    statistics.setExpectedDataOutputSamples(results.size());
    // store results
    missing.clear();
    missing.addAll(outputs);
    for (ComputedValue result : results) {
      final ValueSpecification resultSpec = result.getSpecification();
      if (!missing.remove(resultSpec)) {
        s_logger.debug("Function produced non-requested result {}", resultSpec);
      }
    }
    if (!missing.isEmpty()) {
      final Collection<ComputedValue> newResults = new ArrayList<ComputedValue>(results.size() + missing.size());
      newResults.addAll(results);
      for (ValueSpecification output : missing) {
        newResults.add(new ComputedValue(output, NotCalculatedSentinel.getInstance()));
      }
      results = newResults;
    }
    cache.putValues(results, statistics);
  }
}
