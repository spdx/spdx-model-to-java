/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

/**
 *
 * Exception converting OWL to Java
 *
 * @author gary
 *
 */
public class ShaclToJavaException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * @param msg the message
	 */
	public ShaclToJavaException(String msg) {
		super(msg);
	}


	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ShaclToJavaException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}


	/**
	 * @param message
	 * @param cause
	 */
	public ShaclToJavaException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}


	/**
	 * @param cause
	 */
	public ShaclToJavaException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
