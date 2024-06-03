/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2024 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.engine.constraint.ClosedConstraint;
import org.apache.jena.shacl.engine.constraint.ConstraintComponentSPARQL;
import org.apache.jena.shacl.engine.constraint.DatatypeConstraint;
import org.apache.jena.shacl.engine.constraint.DisjointConstraint;
import org.apache.jena.shacl.engine.constraint.EqualsConstraint;
import org.apache.jena.shacl.engine.constraint.HasValueConstraint;
import org.apache.jena.shacl.engine.constraint.InConstraint;
import org.apache.jena.shacl.engine.constraint.JLogConstraint;
import org.apache.jena.shacl.engine.constraint.JViolationConstraint;
import org.apache.jena.shacl.engine.constraint.LessThanConstraint;
import org.apache.jena.shacl.engine.constraint.LessThanOrEqualsConstraint;
import org.apache.jena.shacl.engine.constraint.MaxCount;
import org.apache.jena.shacl.engine.constraint.MinCount;
import org.apache.jena.shacl.engine.constraint.NodeKindConstraint;
import org.apache.jena.shacl.engine.constraint.PatternConstraint;
import org.apache.jena.shacl.engine.constraint.QualifiedValueShape;
import org.apache.jena.shacl.engine.constraint.ShAnd;
import org.apache.jena.shacl.engine.constraint.ShNode;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.engine.constraint.ShOr;
import org.apache.jena.shacl.engine.constraint.ShXone;
import org.apache.jena.shacl.engine.constraint.SparqlConstraint;
import org.apache.jena.shacl.engine.constraint.StrLanguageIn;
import org.apache.jena.shacl.engine.constraint.StrMaxLengthConstraint;
import org.apache.jena.shacl.engine.constraint.StrMinLengthConstraint;
import org.apache.jena.shacl.engine.constraint.UniqueLangConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMaxExclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMaxInclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMinExclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMinInclusiveConstraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;

/**
 * @author gary
 *
 */
class ConstraintCollector implements ConstraintVisitor {
	
	private Integer minCardinality = null;
	private Integer maxCardinality = null;
	private String pattern = null;
	private Node dataType = null;
	private Integer strMinLengh = null;
	private Integer strMaxLenght = null;
	private Node expectedClass = null;
	private SparqlConstraint sparqlConstraint = null;
	

	@Override
	public void visit(ClassConstraint constraint) {
		expectedClass = constraint.getExpectedClass();
	}

	@Override
	public void visit(DatatypeConstraint constraint) {
		dataType = constraint.getDatatype();
	}

	@Override
	public void visit(NodeKindConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(MinCount constraint) {
		minCardinality = constraint.getMinCount();
	}

	@Override
	public void visit(MaxCount constraint) {
		maxCardinality = constraint.getMaxCount();
	}

	@Override
	public void visit(ValueMinExclusiveConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(ValueMinInclusiveConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(ValueMaxInclusiveConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(ValueMaxExclusiveConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(StrMinLengthConstraint constraint) {
		strMinLengh = constraint.getMinLength();
	}

	@Override
	public void visit(StrMaxLengthConstraint constraint) {
		strMaxLenght = constraint.getMaxLength();
	}

	@Override
	public void visit(PatternConstraint constraint) {
		pattern = constraint.getPattern();
	}

	@Override
	public void visit(StrLanguageIn constraint) {
		// ignore
	}

	@Override
	public void visit(UniqueLangConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(EqualsConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(DisjointConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(LessThanConstraint constraint) {
		// ignore
	}

	@Override
	public void visit(LessThanOrEqualsConstraint constraint) {
		// ignore
		
	}

	@Override
	public void visit(ShNot constraint) {
		// ignore
		
	}

	@Override
	public void visit(ShAnd constraint) {
		// ignore
		
	}

	@Override
	public void visit(ShOr constraint) {
		// ignore
		
	}

	@Override
	public void visit(ShXone constraint) {
		// ignore
		
	}

	@Override
	public void visit(ShNode constraint) {
		// ignore
		
	}

	@Override
	public void visit(QualifiedValueShape constraint) {
		// ignore
		
	}

	@Override
	public void visit(ClosedConstraint constraint) {
		// ignore
		
	}

	@Override
	public void visit(HasValueConstraint constraint) {
		// ignore
		
	}

	@Override
	public void visit(InConstraint constraint) {
		// ignore
		
	}

	@Override
	public void visit(ConstraintComponentSPARQL constraint) {
		// ignore
		
	}

	@Override
	public void visit(SparqlConstraint constraint) {
		this.sparqlConstraint = constraint;
	}

	@Override
	public void visit(JViolationConstraint constraint) {
		// ignore
		
	}

	@Override
	public void visit(JLogConstraint constraint) {
		// ignore
		
	}

	/**
	 * @return minimum constraint
	 */
	public Integer getMinCardinality() {
		return minCardinality;
	}

	/**
	 * @return the maxCardinality
	 */
	public Integer getMaxCardinality() {
		return maxCardinality;
	}

	/**
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * @return the dataType
	 */
	public Node getDataType() {
		return dataType;
	}

	/**
	 * @return the strMinLengh
	 */
	public Integer getStrMinLengh() {
		return strMinLengh;
	}

	/**
	 * @return the strMaxLenght
	 */
	public Integer getStrMaxLenght() {
		return strMaxLenght;
	}

	/**
	 * @return the expectedClass
	 */
	public Node getExpectedClass() {
		return expectedClass;
	}

	/**
	 * @return the sparqlConstraint
	 */
	public SparqlConstraint getSparqlConstraint() {
		return sparqlConstraint;
	}
}
