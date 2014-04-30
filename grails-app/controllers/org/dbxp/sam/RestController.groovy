package org.dbxp.sam

import grails.converters.JSON
import dbnp.studycapturing.*
import org.dbnp.gdt.RelTime
import org.dbnp.gdt.TemplateEntity
import groovy.sql.Sql

class RestController {
    def dataSource

	/****************************************************************/
	/* REST resources for providing basic data to the GSCF          */
	/****************************************************************/

	/**
	 * Return a list of simple assay measurements matching the querying text.
	 *
	 * @param assayToken
	 * @return list of feature names for assay.
	 *
	 * Example REST call:
	 * http://localhost:8184/metagenomics/rest/getMeasurements/query?assayToken=16S-5162
	 *
	 * Resulting JSON object:
	 *
	 * [ "# sequences", "average quality" ]
	 *
	 */
	def getMeasurements = {
		def assayToken = params.assayToken;
		def assay = getAssay( assayToken );
		if( !assay ) {
			response.sendError(404)
			return false
		}

		// Return all features for the given assay
		def features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s WHERE m.feature = f AND m.sample = s AND s.parentAssay = :assay", [ "assay": assay ] )

		render features.collect { it.name } as JSON
	}

    def getQueryableFieldData = {

        println "wel hier3332!!!!!!!!!!!!!!!!!!!!!!!!!"

        def entityClass = TemplateEntity.parseEntity( 'dbnp.studycapturing.' + params.entity)

        def idMap = [:]
        entityClass.list().each() {
            idMap[it.UUID] = it
        }

        println idMap



        println params

        render features.collect { it.name } as JSON

//        def assayToken = params.assayToken;
//        def assay = getAssay( assayToken );
//        if( !assay ) {
//            response.sendError(404)
//            return false
//        }
//
//        // Return all features for the given assay
//        def features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s WHERE m.feature = f AND m.sample = s AND s.parentAssay = :assay", [ "assay": assay ] )
//
//        render features.collect { it.name } as JSON
    }

	/**
	 * Return measurement metadata for measurement
	 *
	 * @param assayToken
	 * @param measurementTokens. List of measurements for which the metadata is returned.
	 *                           If this is not given, then return metadata for all
	 *                           measurements belonging to the specified assay.
	 * @return list of measurements
	 *
	 * Example REST call:
	 * http://localhost:8184/metagenomics/rest/getMeasurementMetadata/query?assayToken=16S-5162
	 *      &measurementToken=# sequences
	 *		&measurementToken=average quality
	 *
	 * Example resulting JSON object:
	 *
	 * [ {"name":"# sequences","type":"raw"},
	 *   {"name":"average quality", "unit":"Phred"} ]
	 */
	def getMeasurementMetaData = {
		def assayToken = params.assayToken;
		def assay = getAssay( assayToken );
		if( !assay ) {
			response.sendError(404)
			return false
		}
		
		def measurementTokens = params.list( 'measurementToken' );
		def features
		
		if( measurementTokens ) {
			// Return all requested features for the given assay
			features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s WHERE m.sample = s AND m.feature = f AND s.parentAssay = :assay AND f.name IN (:measurementTokens)", [ "assay": assay, "measurementTokens": measurementTokens ] )
		} else {
			// If no measurement tokens are given, return values for all features
			features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s WHERE m.sample = s AND m.feature = f AND s.parentAssay = :assay", [ "assay": assay ] )
		}
		
		render features.collect { feature -> 
			def obj = [:];
			feature.giveFields().each { field ->
				obj[ field.name ] = feature.getFieldValue( field.name );
			}
			return obj;
		} as JSON
	}

	/**
	 * Return list of measurement data.
	 *
	 * @param assayToken
	 * @param measurementToken. Restrict the returned data to the measurementTokens specified here.
	 * 						If this argument is not given, all samples for the measurementTokens are returned.
	 * 						Multiple occurrences of this argument are possible.
	 * @param sampleToken. Restrict the returned data to the samples specified here.
	 * 						If this argument is not given, all samples for the measurementTokens are returned.
	 * 						Multiple occurrences of this argument are possible.
	 * @param boolean verbose. If this argument is not present or it's value is true, then return
	 *                    getAssay  the date in a redundant format that is easier to process.
	 *						By default, return a more compact JSON object as follows.
	 *
	 * 						The list contains three elements:
	 *
	 *						(1) a list of sampleTokens,
	 *						(2) a list of measurementTokens,
	 * 						(3) a list of values.
	 *
	 * 						The list of values is a matrix represented as a list. Each row of the matrix
	 * 						contains the values of a measurementToken (in the order given in the measurement
	 * 						token list, (2)). Each column of the matrix contains the values for the sampleTokens
	 * 						(in the order given in the list of sampleTokens, (1)).
	 * 						(cf. example below.)
	 *
	 *
	 * @return  table (as hash) with values for given samples and measurements
	 *
	 *
	 * List of examples.
	 *
	 *
	 * Example REST call:
	 * http://localhost:8184/metagenomics/rest/getMeasurementData/doit?assayToken=PPSH-Glu-A
	 *    &measurementToken=total carbon dioxide (tCO)
	 *    &sampleToken=5_A
	 *    &sampleToken=1_A
	 *    &verbose=true
	 *
	 * Resulting JSON object:
	 * [ {"sampleToken":"1_A","measurementToken":"total carbon dioxide (tCO)","value":28},
	 *   {"sampleToken":"5_A","measurementToken":"total carbon dioxide (tCO)","value":29} ]
	 *
	 *
	 *
	 * Example REST call without sampleToken, without measurementToken,
	 *    and with verbose representation:
	 * http://localhost:8184/metagenomics/rest/getMeasurementData/dossit?assayToken=PPSH-Glu-A
	 *    &verbose=true
	 *
	 * Resulting JSON object:
	 * [ {"sampleToken":"1_A","measurementToken":"sodium (Na+)","value":139},
	 *	 {"sampleToken":"1_A","measurementToken":"potassium (K+)","value":4.5},
	 *	 {"sampleToken":"1_A","measurementToken":"total carbon dioxide (tCO)","value":26},
	 *	 {"sampleToken":"2_A","measurementToken":"sodium (Na+)","value":136},
	 *	 {"sampleToken":"2_A","measurementToken":"potassium (K+)","value":4.3},
	 *	 {"sampleToken":"2_A","measurementToken":"total carbon dioxide (tCO)","value":28},
	 *	 {"sampleToken":"3_A","measurementToken":"sodium (Na+)","value":139},
	 *	 {"sampleToken":"3_A","measurementToken":"potassium (K+)","value":4.6},
	 *	 {"sampleToken":"3_A","measurementToken":"total carbon dioxide (tCO)","value":27},
	 *	 {"sampleToken":"4_A","measurementToken":"sodium (Na+)","value":137},
	 *	 {"sampleToken":"4_A","measurementToken":"potassium (K+)","value":4.6},
	 *	 {"sampleToken":"4_A","measurementToken":"total carbon dioxide (tCO)","value":26},
	 *	 {"sampleToken":"5_A","measurementToken":"sodium (Na+)","value":133},
	 *	 {"sampleToken":"5_A","measurementToken":"potassium (K+)","value":4.5},
	 *	 {"sampleToken":"5_A","measurementToken":"total carbon dioxide (tCO)","value":29} ]
	 *
	 *
	 *
	 * Example REST call with default (non-verbose) view and without sampleToken:
	 *
	 * Resulting JSON object:
	 * http://localhost:8184/metagenomics/rest/getMeasurementData/query?
	 * 	assayToken=PPSH-Glu-A&
	 * 	measurementToken=sodium (Na+)&
	 * 	measurementToken=potassium (K+)&
	 *	measurementToken=total carbon dioxide (tCO)
	 *
	 * Resulting JSON object:
	 * [ ["1_A","2_A","3_A","4_A","5_A"],
	 *   ["sodium (Na+)","potassium (K+)","total carbon dioxide (tCO)"],
	 *   [139,136,139,137,133,4.5,4.3,4.6,4.6,4.5,26,28,27,26,29] ]
	 *
	 * Explanation:
	 * The JSON object returned by default (i.e., unless verbose is set) is an array of three arrays.
	 * The first nested array gives the sampleTokens for which data was retrieved.
	 * The second nested array gives the measurementToken for which data was retrieved.
	 * The thrid nested array gives the data for sampleTokens and measurementTokens.
	 *
	 *
	 * In the example, the matrix represents the values of the above Example and
	 * looks like this:
	 *
	 * 			1_A		2_A		3_A		4_A		5_A
	 *
	 * Na+		139		136		139		137		133
	 *
	 * K+ 		4.5		4.3		4.6		4.6		4.5
	 *
	 * tCO		26		28		27		26		29
	 *
	 */
	def getMeasurementData = {
		def verbose = false
		
		if(params.verbose && (params.verbose=='true'||params.verbose==true) ) {
			verbose=true
		}
		
		def assayToken = params.assayToken;
		def assay = getAssay( assayToken );
		if( !assay ) {
			response.sendError(404)
			return false
		}
		
		def measurementTokens = params.list( 'measurementToken' );
		def sampleTokens = params.list( 'sampleToken' );
		
		def features
		def samples
		def results
		
		if( measurementTokens ) {
			// Return all requested features for the given assay
			features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s WHERE m.sample = s AND m.feature = f AND s.parentAssay = :assay AND f.name IN (:measurementTokens)", [ "assay": assay, "measurementTokens": measurementTokens ] )
			log.debug("Found ${features.size()} features matching the ${measurementTokens.size()} measurement tokens")		
		} else {
			// If no measurement tokens are given, return values for all features
			features = Feature.executeQuery( "SELECT DISTINCT f FROM Feature f, Measurement m, SAMSample s  WHERE m.sample = s AND m.feature = f AND s.parentAssay = :assay", [ "assay": assay ] )
			log.debug("Using all ${features.size()} features")
		}
		
		if( sampleTokens ) {
			// Return all requested samples
			samples = SAMSample.executeQuery( "SELECT s FROM SAMSample s WHERE s.parentAssay = :assay AND s.parentSample.UUID IN (:sampleTokens)", [ "assay": assay, "sampleTokens": sampleTokens ] )
			log.debug("Found ${samples.size()} samples matching the ${sampleTokens.size()} sample tokens")
			/*log.debug("i got this: ${sampleTokens}")
			def alles = Sample.executeQuery( "SELECT s.sampleToken FROM Sample s WHERE s.assay = :assay", [ "assay": assay] )
			log.debug("alles is ${alles}")
			alles.each {
					log.debug(it.dump())
			}*/
		} else {
			// If no sampleTokens are given, return all	
			samples = assay.samples;
			log.debug("Using all ${samples.size()} samples")
		}
		
		// If no samples or features are selected, return an empty list
		if( !samples || !features ) {
			results = []
			log.debug("No samples or no features, returning empty result")
		}
		else {
		
			// Retrieve all measurements from the database
            def measurements = Measurement.executeQuery("SELECT m, m.feature, m.sample FROM Measurement m WHERE m.feature IN (:features) AND m.sample IN (SELECT s FROM SAMSample s WHERE s.parentAssay = :assay)", ["assay": assay, "features": features])

			// Convert the measurements into the desired format
			results = measurements.collect {
                [
				    "sampleToken": 		it[ 2 ].parentSample.UUID,
				    "measurementToken": it[ 1 ].name + ( it[ 1 ].unit ? " (" + it[1].unit + ")" : "" ),
				    "value":			it[ 0 ].value
			    ]
            }

			if(!verbose) {
				results = compactTable( results )
			}
		}
		render results as JSON
    }

    def getFeaturesForAssay = {
        def assayToken = params.assayToken;
        def assay = getAssay( assayToken );
        if( !assay ) {
            response.sendError(404)
            return false
        }

        def sql = new Sql(dataSource)

        def features = sql.rows("SELECT f.name, f.unit, fs.template_string_fields_idx, fs.template_string_fields_elt, ft.template_text_fields_idx, ft.template_text_fields_elt, p.name AS platform, p.platformtype, p.platformversion FROM feature AS f LEFT JOIN feature_template_string_fields fs ON (f.id = fs.feature_id) LEFT JOIN feature_template_text_fields ft ON (f.id = ft.feature_id) LEFT JOIN platform p ON (f.platform_id = p.id) WHERE f.id IN (SELECT DISTINCT feature_id FROM measurement WHERE sample_id IN (SELECT id FROM samsample WHERE parent_assay_id = ${assay.id})) ORDER BY f.name ASC;")

        def fMap = [:]
        def propertyMap = [:]
        def i = 0
        features.each() { f ->
            i++
            if (f.template_string_fields_idx) {
                propertyMap.put(f.template_string_fields_idx, f.template_string_fields_elt)
            }
            if (f.template_text_fields_idx) {
                propertyMap.put(f.template_text_fields_idx, f.template_text_fields_elt)
            }
            if (!f.name.equals(features[i]?.name)) {
                propertyMap.put("unit", f.unit)
                propertyMap.put("platform", f.platform)
                propertyMap.put("platformtype", f.platformtype)
                propertyMap.put("platformversion", f.platformversion)
                fMap.put(f.name, propertyMap)
                propertyMap = [:]
            }
        }

        features.clear()
        propertyMap.clear()

        render fMap as JSON
    }

    /**
     * Retrieves an assay from the database, based on a given assay token.
     * @param assayToken	Assaytoken for the assay to retrieve
     * @return				Assay or null if assayToken doesn't exist
     */
    def getPlainMeasurementData = {
        def assayToken = params.assayToken;
        def assay = getAssay( assayToken );
        if( !assay ) {
            response.sendError(404)
            return false
        }

        def sql = new Sql(dataSource)

        def pMeasurements = sql.rows("SELECT s.parent_subject_id AS subject, e.start_time AS startTime, m.feature_id AS feature, m.value AS value FROM measurement m, sample s, sampling_event e, samsample y WHERE m.sample_id = y.id AND s.parent_event_id = e.id AND y.parent_sample_id = s.id AND y.parent_assay_id = ${assay.id} ORDER BY s.parent_subject_id ASC, e.start_time DESC")

        def subjectMap = assay.parent.subjects.collectEntries{ [it.id, it.name] }

        def featureMap = sql.rows("SELECT DISTINCT m.feature_id, f.name FROM measurement m JOIN feature f ON m.feature_id = f.id WHERE m.sample_id IN (SELECT id FROM samsample s WHERE s.parent_assay_id = ${assay.id});").collectEntries{ [it.feature_id, it.name] }

        // map for all measurements
        def allMap = [:]
        // map for subject + startTime measurements (goes in allMap)
        def groupMap = [:]
        // map for subject measurements (goes in groupMap)
        def mMap = [:]
        def i = 0
        pMeasurements.each() { m ->
            i++
            mMap.put(featureMap.get(m.feature), m.value)
            if (!m.subject.equals(pMeasurements[i]?.subject)) {
                groupMap.put(new RelTime(m.starttime), mMap)
                allMap.put(subjectMap.get(m.subject), groupMap)
                groupMap = [:]
                mMap = [:]

            }
            else if (m.starttime != pMeasurements[i]?.starttime) {
                groupMap.put(new RelTime(m.startTime), mMap)
                mMap = [:]
            }
        }
        pMeasurements.clear()
        subjectMap.clear()
        featureMap.clear()
        groupMap.clear()
        mMap.clear()

        render allMap as JSON
	}

	/**
	 * Retrieves an assay from the database, based on a given assay token.
	 * @param assayToken	Assaytoken for the assay to retrieve
	 * @return				Assay or null if assayToken doesn't exist
	 */
	private def getAssay( def assayToken ) {
		if( !assayToken || assayToken == null ) {
			return null
		}
		def list = []
		def assay = Assay.findWhere(UUID: assayToken )

		return assay;
	}


    /* helper function for getMeasurementData
     *
     * Return compact JSON object for data. The format of the returned array is as follows.
     *
     * The list contains three elements:
     *
     * (1) a list of sampleTokens,
     * (2) a list of measurementTokens,
     * (3) a list of values.
     *
     * The list of values is a matrix represented as a list. Each row of the matrix
     * contains the values of a measurementToken (in the order given in the measurement
     * token list, (2)). Each column of the matrix contains the values for the sampleTokens
     * (in the order given in the list of sampleTokens, (1)).
     */
    private def compactTable( results ) {
        def sampleTokens = results.collect( { it['sampleToken'] } ).unique()
        def measurementTokens = results.collect( { it['measurementToken'] } ).unique()

        def data = []
        measurementTokens.each{ m ->
            sampleTokens.each{ s ->
                def item = results.find{ it['sampleToken']==s && it['measurementToken']==m }
                data.push item ? item['value'] : null
            }
        }

        return [ sampleTokens, measurementTokens, data ]
    }
}
