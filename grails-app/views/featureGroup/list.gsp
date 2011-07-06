<%@ page import="org.dbxp.sam.FeatureGroup" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <g:set var="entityName" value="${message(code: 'featureGroup.label', default: 'FeatureGroup')}"/>
        <title><g:message code="default.list.label" args="[entityName]"/></title>
        <script type="text/javascript">
            $(document).ready(function() {
                $('#fgList').dataTable();
            } );

            function deleteItems(){
                var selected_boxes = $("input[@name=fgMassDelete]:checked");
                var num = selected_boxes.length;
                var values = $(selected_boxes).val();
                alert(num,+", "+values);
            }

            function submitForm(id, action) {
                var form = $( 'form#' + id );

                if( action != undefined ) {
                    $( 'input[name=event]', form ).val( action );
                    $( 'input[name=_eventId]', form ).val( action );
                }

                form.submit();
            }
        </script>
    </head>
    <body>
        <div class="body">
            <h1><g:message code="default.list.label" args="[entityName]"/></h1>
            <g:form name="deleteMultiple" action="deleteMultiple">
                <div class="list">
                    <table id="fgList">
                        <thead>
                        <tr>
                            <th>${message(code: 'featureGroup.name.label', default: 'Name')}</th>

                            <th>In use</th>

                            <th>Mark for deletion</th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${featureGroupInstanceList}" status="i" var="featureGroupInstance">
                            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                                <td><g:link action="show"
                                            id="${featureGroupInstance.id}">${fieldValue(bean: featureGroupInstance, field: "name")}</g:link></td>

                                <td>
                                    <%
                                        def a = org.dbxp.sam.FeaturesAndGroups.findAllByFeatureGroup(featureGroupInstance)
                                        def b = ""
                                        if(a!=null && a.size()>0){
                                            if(a.size()>1){
                                                b = "Yes, used by "+a.size()+" features"
                                            } else {
                                                b = "Yes, used by one feature"
                                            }
                                        } else {
                                            b = "No, not in use"
                                        }
                                    %>
                                    ${b}
                                </td>

                                <td>
                                    <input type="checkbox" name="fgMassDelete" value="${featureGroupInstance.id}"/>
                                </td>

                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
                <br>
                <div class="samNav">
                    <span class="addItem"><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]"/></g:link></span>
                    <span class="simpleButton deleteItem">
                        <a onclick="submitForm('deleteMultiple', '');">
                            <g:message code="default.button.delete.label" args="[entityName]"/>
                        </a>
                    </span>
                </div>
            </g:form>
        </div>
    </body>
</html>