package org.dbxp.sam

import grails.converters.JSON
import org.dbnp.gdt.Template
import grails.converters.deep.JSON
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class FeatureController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }
	
    def list = {
        [featureInstanceList: Feature.list(params), featureInstanceTotal: Feature.count()]
    }

    def create = {
        def featureInstance = new Feature()
        featureInstance.properties = params
        return [featureInstance: featureInstance]
    }

    def save = {
        def featureInstance = new Feature(params)
        if (featureInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'feature.label', default: 'Feature'), featureInstance.id])}"
            if(params?.nextPage=="edit"){
                redirect(action: "edit", id: featureInstance.id, featureInstance: featureInstance)
            } else {
                redirect(action: "list")
            }
        }
        else {
            render(view: "create", model: [featureInstance: featureInstance])
        }
    }

    def show = {
        def featureInstance = Feature.get(params.id)
        if (!featureInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
            redirect(action: "list")
        }
        else {
            [featureInstance: featureInstance]
        }
    }

    def edit = {
        def featureInstance = Feature.get(params.id)
        session.featureInstance = featureInstance
        if (!featureInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [featureInstance: featureInstance]
        }
    }

    def update = {
        def featureInstance = Feature.get(params.id)
        if (featureInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (featureInstance.version > version) {

                    featureInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'feature.label', default: 'Feature')] as Object[], "Another user has updated this Feature while you were editing")
                    render(view: "edit", model: [featureInstance: featureInstance])
                    return
                }
            }
			
			// did the study template change?
			if( params.template && params.template != featureInstance.template?.name ) {
				featureInstance.changeTemplate( params.template );
			}

            // does the study have a template set?
            if (featureInstance.template) {
                // yes, iterate through template fields
                featureInstance.giveFields().each() {
                    // and set their values
                    featureInstance.setFieldValue(it.name, params.get(it.escapedName()))
                }
            }

			// Remove the template parameter, since it is a string and that troubles the 
			// setting of properties.
			params.remove( 'template' ) 
			
            featureInstance.properties = params
            if (!featureInstance.hasErrors() && featureInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'feature.label', default: 'Feature'), featureInstance.id])}"
                redirect(action: "show", id: featureInstance.id)
            }
            else {
                render(view: "edit", model: [featureInstance: featureInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def featureInstance = Feature.get(params.id)
        if (featureInstance) {
            def FaGList = FeaturesAndGroups.findAllByFeature(featureInstance)
            try {
                if(FaGList.size()!=0){
                    FaGList.each {
                        it.delete(flush: true)
                    }
                }
                featureInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.error(e)
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])}"
            redirect(action: "list")
        }
    }

    def refreshEdit = {
        // Used to refresh the edit page after a change of template
        if(!session.featureInstance.isAttached()){
            session.featureInstance.attach()
        }
        updateTemplate()
        render(view: "edit", model: [featureInstance: session.featureInstance])
    }

    def confirmNewFeatureGroup = {
        // Used to add a new FeaturesAndGroups connection and to refresh the edit page's feature group list
        if(!session.featureInstance.isAttached()){
            session.featureInstance.attach()
        }
        if(params?.newFeatureGroupID) {
            // Creating a new group
            FeaturesAndGroups.create(FeatureGroup.get(params.newFeatureGroupID), session.featureInstance);
        }

        // This featureInstance is only used to display an accurate list
        def featureInstance = Feature.get(params.id)
        render(view: "FaGList", model: [featureInstance: featureInstance])
    }

    def templateSpecific = {
        // Get a list of template specific fields
        def featureInstance = Feature.get(params.id)
        render(view: "templateSpecific", model: [featureInstance: featureInstance])
    }

    def templateSelection = {
        // Get a template selector
        // Actually using this in a .gsp does not seem to lead to a working template editor
        // TODO: fix this so that add/modify does not have to lead to a page refresh
        def featureInstance = Feature.get(params.id)
        render(view: "templateSelection", model: [featureInstance: featureInstance])
    }

	def updateTemplate = {
        // A different template has been selected, so all the template fields have to be removed, added or updated with their previous values (they start out empty)
       if(!session.featureInstance.isAttached()){
           session.featureInstance.attach()
       }
       try {
            if(params.template==""){
                println "Removing template..."
                session.featureInstance.template = null
            } else if(params?.template && session?.featureInstance.template?.name != params.get('template')) {
               // set the template
                println "params.template : "+params.template
               session.featureInstance.template = Template.findByName(params.template)
           }

            println "Updating template..."
           // does the study have a template set?
           if (session.featureInstance.template && session.featureInstance.template instanceof Template) {
               // yes, iterate through template fields
               session.featureInstance.giveFields().each() {
                   // and set their values
                   session.featureInstance.setFieldValue(it.name, params.get(it.escapedName()))
               }
           }
       } catch (Exception e){
           log.error(e)
            e.printStackTrace()
           // TODO: Make this more informative
           flash.message = "An error occurred while updating this feature's template. Please try again.<br>${e}"
       }
   }

    def removeGroup = {
        // Used to delete a FeaturesAndGroups connection
        if(!session.featureInstance.isAttached()){
            session.featureInstance.attach()
        }
        def featuresAndGroupsInstance
        try {
            // Try to find the featuresAndGroupsInstance that we wish to delete
            featuresAndGroupsInstance = FeaturesAndGroups.get(params.fagid)
        } catch (Exception e){
            log.error(e)
            // An error occurred while fetching the featuresAndGroupsInstance that we wish to remove
            flash.message = "The specified group could not be found.<br>${e}"
            if(params?.id){
                render(view: "edit", model: [featureInstance: session.featureInstance, id: params.id])
            } else {
                redirect(action: "list")
            }
        }
        if(featuresAndGroupsInstance==null){
            // We could not find the featuresAndGroupsInstance that we wish to remove
            flash.message = "The specified group could not be found. The probable cause for this would be that the group has already been removed."
            def id
            if(params?.id){
                id = params.id
            } else if(session?.featureInstance.id){
                id = session.featureInstance.id
            }

            if(id){
                def featureInstance = Feature.get(id)
                render(view: "edit", model: [featureInstance: featureInstance, id: id])
            } else {
                redirect(action: "list")
            }
        } else {
            // We found the featuresAndGroupsInstance that we wish to remove

            def groupName = featuresAndGroupsInstance.featureGroup.name
            def featureID = featuresAndGroupsInstance.feature.id

            // Proceed with deletion
            try {
                featuresAndGroupsInstance.delete(flush: true)
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.error(e)
                flash.message = "There has been a problem with removing the group ${groupName} from this feature.<br>${e}"
                redirect(action: "edit", id: featureID, featureInstance: session.featureInstance)
            }

            // Deletion of the featuresAndGroupsInstance was successful, so we will try to get back to this Feature's editing page
            redirect(action: "edit", id: featureID, featureInstance: session.featureInstance)
        }
    }

    def deleteMultiple = {
        // Used to delete multiple features from the feature list
        def toDeleteList = []
        if(params?.fMassDelete!=null){
            // If necessary, go from a string to a list of strings
            if(params.fMassDelete.class!="".class){
                toDeleteList = params.fMassDelete
            } else {
                toDeleteList.push(params.fMassDelete)
            }
        }
        if(toDeleteList.size()>0){
            def return_map = [:]
            return_map = Feature.deleteMultipleFeatures(toDeleteList)
            def error = return_map.get("error")
            if(error){
                log.error(error)
            }
            def message = return_map.get("message")
            if(message){
                flash.message = message
            }
            def action = return_map.get("action")
            if(action){
                redirect(action: action)
            } else {
                redirect(action:list)
            }
        } else {
            flash.message = "No features were marked when the delete button was clicked, so no features were deleted."
            redirect(action: "list")
        }
    }
}