<%@ page import="org.dbxp.sam.Feature" %>
<%@ page import="org.dbnp.gdt.GdtTagLib" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>

        <g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}"/>
        <title><g:message code="default.edit.label" args="[entityName]"/></title>
        <script>
            $(document).ready(function() {
                $('#test-div').html("Available feature templates: "+${Feature.list().template});
                //alert("Did it werk? Also, "+$('#test-div').html());
                new SelectAddMore().init({
                    rel	 : 'template',
                    url	 : baseUrl + '/templateEditor',
                    vars	: 'entity,ontologies',
                    label   : 'add / modify..',
                    style   : 'modify',
                    onClose : function(scope) {}it.escapedName()
                });
            });
        </script>
    </head>

    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a>
            </span>
            <span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label"
                                                                                   args="[entityName]"/></g:link></span>
            <span class="menuButton"><g:link class="create" action="create"><g:message code="default.new.label"
                                                                                       args="[entityName]"/></g:link></span>
        </div>

        <div class="body">
            <h1><g:message code="default.edit.label" args="[entityName]"/></h1>
            <g:if test="${flash.message}">
                <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${featureInstance}">
                <div class="errors">
                    <g:renderErrors bean="${featureInstance}" as="list"/>
                </div>it.escapedName()
            </g:hasErrors>
            <g:form method="post">
                <g:hiddenField name="id" value="${featureInstance?.id}"/>
                <g:hiddenField name="version" value="${featureInstance?.version}"/>
                <div class="dialog">
                    <table>
                        <tbody>

                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="name"><g:message code="feature.name.label" default="Name"/></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: featureInstance, field: 'name', 'errors')}">
                                    <g:textField name="name" value="${featureInstance?.name}"/>
                                </td>
                            </tr>

                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="featureGroups"><g:message code="feature.featureGroups.label"
                                                                          default="Feature Groups"/></label>
                                </td>
                                <td valign="top"
                                    class="value ${hasErrors(bean: featureInstance, field: 'featureGroups', 'errors')}">

                                </td>
                            </tr>

                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="unit"><g:message code="feature.unit.label" default="Unit"/></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: featureInstance, field: 'unit', 'errors')}">
                                    <g:textField name="unit" value="${featureInstance?.unit}"/>
                                </td>
                            </tr>

                            <tr class="prop">
                                <td valign="top" class="name">
                                    Template, first try
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: featureInstance, field: 'template', 'errors')}">
                                    <af:templateElement name="template" rel="template" description="with template" value="${featureInstance?.template}" error="template" entity="${Feature}" ontologies="${featureInstance.giveDomainFields()}" addDummy="true">
                                        The template to use for this feature.
                                    </af:templateElement>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="buttons">
                    <span class="button"><g:actionSubmit class="save" action="update"
                                                         value="${message(code: 'default.button.update.label', default: 'Update')}"/></span>
                    <span class="button"><g:actionSubmit class="delete" action="delete"
                                                         value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                                                         onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/></span>
                </div>
            </g:form>
        </div>
        <div id="test-div">
            ...
        </div>
        <div id="test-div_2">
            ${featureInstance.giveDomainFields()}
        </div>
    </body>
</html>
