/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.depgraph;

import org.junit.Assert;
import org.junit.Test;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionDefinition;
import com.opengamma.engine.function.MockFunction;
import com.opengamma.id.IdentificationScheme;
import com.opengamma.id.Identifier;

/**
 * Test DependencyNode.
 */
public class DependencyNodeTest {
  
  @Test
  public void testDependentNodes() {
    IdentificationScheme domain = new IdentificationScheme("test");
    
    FunctionDefinition function = new MockFunction(new ComputationTarget(ComputationTargetType.PRIMITIVE, "USD"));
    
    FunctionCompilationContext context = new FunctionCompilationContext();
    DependencyNode node0 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "0")));
    DependencyNode node1 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "1")));
    DependencyNode node2 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "2")));
    DependencyNode node3 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "3")));
    DependencyNode node4 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "4")));
    DependencyNode node5 = new DependencyNode(context, function, new ComputationTarget(ComputationTargetType.PRIMITIVE, new Identifier(domain, "5")));
    node0.addInputNode(node1);
    node0.addInputNode(node2);
    node0.addInputNode(node3);
    node2.addInputNode(node1);
    node2.addInputNode(node3);
    node4.addInputNode(node3);
    Assert.assertEquals(0, node5.getDependentNodes().size());
    Assert.assertEquals(1, node2.getDependentNodes().size());
    Assert.assertTrue(node2.getDependentNodes().contains(node0));
    Assert.assertEquals(2, node1.getDependentNodes().size());
    Assert.assertTrue(node1.getDependentNodes().contains(node0));
    Assert.assertTrue(node1.getDependentNodes().contains(node2));
    Assert.assertEquals(3, node3.getDependentNodes().size());
    Assert.assertTrue(node3.getDependentNodes().contains(node0));
    Assert.assertTrue(node3.getDependentNodes().contains(node2));
    Assert.assertTrue(node3.getDependentNodes().contains(node4));
    node4.addInputNode(node3); // test the set bit
    Assert.assertEquals(3, node3.getDependentNodes().size());
    Assert.assertTrue(node3.getDependentNodes().contains(node0));
    Assert.assertTrue(node3.getDependentNodes().contains(node2));
    Assert.assertTrue(node3.getDependentNodes().contains(node4));    
  }
}
