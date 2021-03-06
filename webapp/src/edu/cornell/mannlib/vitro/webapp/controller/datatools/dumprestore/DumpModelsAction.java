/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore;

import static edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore.DumpRestoreController.ACTION_DUMP;
import static edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore.DumpRestoreController.PARAMETER_FORMAT;
import static edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore.DumpRestoreController.PARAMETER_WHICH;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore.DumpRestoreController.BadRequestException;
import edu.cornell.mannlib.vitro.webapp.controller.datatools.dumprestore.DumpRestoreController.DumpFormat;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess.WhichService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService.ResultFormat;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;

/**
 * The user has requested a dump.
 * 
 * First time through, figure out what filename we would like to put on the
 * dump, and send a redirect.
 * 
 * Second time thorugh, actually create the dump.
 */
class DumpModelsAction extends AbstractDumpRestoreAction {
	private static final Log log = LogFactory.getLog(DumpModelsAction.class);

	private final HttpServletResponse resp;
	private final DumpFormat format;
	private final WhichService which;
	private final String queryString;

	DumpModelsAction(HttpServletRequest req, HttpServletResponse resp)
			throws BadRequestException {
		super(req);
		this.resp = resp;
		this.format = getEnumFromParameter(DumpFormat.class, PARAMETER_FORMAT);
		this.which = getEnumFromParameter(WhichService.class, PARAMETER_WHICH);
		this.queryString = req.getQueryString();

	}

	void redirectToFilename() throws IOException {
		String filename = which + "." + format.getExtension();
		String urlPath = req.getContextPath() + req.getServletPath()
				+ ACTION_DUMP;
		resp.sendRedirect(urlPath + "/" + filename + "?" + queryString);
	}

	void dumpModels() {
		try {
			RDFService rdfService = getRdfService(which);
			String query = "SELECT * WHERE { GRAPH ?g {?s ?p ?o}}";

			resp.setContentType(format.getMimeType());
			if (format == DumpFormat.NQUADS) {
				dumpNQuads(rdfService, query);
			} else {
				rdfService.sparqlSelectQuery(query,
						format.getRdfServiceFormat(), resp.getOutputStream());
			}
		} catch (Throwable t) {
			log.error("Failed to dump " + which + " models as " + format + ".",
					t);
		}
	}

	/**
	 * The RDF service won't produce NQuads, so we get JSON and parse it.
	 */
	private void dumpNQuads(RDFService rdfService, String query)
			throws RDFServiceException, IOException {
		JsonToNquads converter = new JsonToNquads(resp.getOutputStream());
		rdfService.sparqlSelectQuery(query, ResultFormat.JSON, converter);
		converter.close();
	}

}
