/**
 * SPDX-FileCopyrightText: Copyright (c) 2023 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.model2java;

/**
 * Exception converting OWL to Java
 *
 * @author Gary O'Neall
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


	public ShaclToJavaException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}


	public ShaclToJavaException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}


	public ShaclToJavaException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
