/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2024 Source Auditor Inc.
 */
package org.spdx.library.model.v3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.spdx.core.CoreModelObject;
import org.spdx.core.IModelCopyManager;
import org.spdx.core.ISpdxModelInfo;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.storage.IModelStore;

/**
 * GENERATED
 *
 */
public class SpdxModelInfoV3_0 implements ISpdxModelInfo {

	@Override
	public Map<String, Enum<?>> getUriToEnumMap() {
		return SpdxEnumFactory.uriToEnum;
	}

	@Override
	public List<String> getSpecVersions() {
		return Arrays.asList(new String[] {"SPDX-3.0"});
	}

	@Override
	public CoreModelObject createExternalElement(IModelStore store, String uri,
			IModelCopyManager copyManager, String specVersion)
			throws InvalidSPDXAnalysisException {
		return new ExternalElement(store, uri, copyManager, true);
	}

	@Override
	public Map<String, Object> getUriToIndividualMap() {
		return SpdxIndividualFactory.uriToIndividual;
	}

	@Override
	public CoreModelObject createModelObject(IModelStore modelStore,
			String objectUri, String type, IModelCopyManager copyManager,
			String specVersion, boolean create)
			throws InvalidSPDXAnalysisException {
		return SpdxModelClassFactory.getModelObject(modelStore, objectUri, type, copyManager, create);
	}

	@Override
	public Map<String, Class<?>> getTypeToClassMap() {
		return SpdxModelClassFactory.SPDX_TYPE_TO_CLASS_V3;
	}

}
