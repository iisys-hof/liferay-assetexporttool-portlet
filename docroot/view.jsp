<%
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://alloy.liferay.com/tld/aui" prefix="aui" %>

<%@ page import="java.util.List" %>

<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portlet.wiki.service.WikiPageLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.wiki.model.WikiPage" %>
<%@ page import="com.liferay.portlet.wiki.model.WikiPageConstants" %>
<%@ page import="com.liferay.portal.service.GroupLocalServiceUtil" %>


<portlet:defineObjects />


<portlet:actionURL var="actionURL">
    <portlet:param name="mvcPath" value="/view.jsp" />
</portlet:actionURL>

<portlet:resourceURL var="ajaxResourceUrl" />



<aui:form id="<portlet:namespace />exportForm" action="<%= actionURL %>" method="post">
	<aui:button type="submit" value="export" style="float:right;" />

	<aui:fieldset>
		<liferay-ui:input-asset-links />
	</aui:fieldset>
	
	<div>
		<aui:input inlineLabel="left" inlineField="<%= true %>" name="exportTo" type="radio" value="pdf"  label="wikiexporttool_pdf" checked="<%= true %>" />
		<aui:input inlineLabel="left" inlineField="<%= true %>" name="exportTo" type="radio" value="docx" label="wikiexporttool_docx" />
	</div>
	
	<% String checkTargetSelFunction = renderResponse.getNamespace()+"checkTargetSelection()"; %>
	<div>
		<aui:input onclick="<%= checkTargetSelFunction %>" inlineField="<%= true %>" name="exportTarget" type="radio" value="download" label="wikiexporttool_download" checked="<%= true %>" />
		<aui:input onclick="<%= checkTargetSelFunction %>" id="exportTargetSendTo" inlineField="<%= true %>" name="exportTarget" type="radio" value="dms"  label="wikiexporttool_sendTo" />	
	</div>
	
	<div id="<portlet:namespace />targetChooser" style="display:none; clear:both; margin-top:10px;">
		<%-- 
		<aui:select name="workspace_dir" label="assetexporttool_targetDirectory">
			<aui:option>/</aui:option>
			<aui:option>/ Ideen</aui:option>
		</aui:select>
		<div>
			<aui:input inlineField="<%= true %>" name="workspace" type="radio" value="personal"  label="assetexporttool_personalWorkspace" checked="<%= true %>" />
			<aui:input inlineField="<%= true %>" name="workspace" type="radio" value="public" label="assetexporttool_publicWorkspace" />
		</div>
		--%>
		<aui:input id="chosenDir" name="chosenDir" type="hidden" />
		<div id="<portlet:namespace/>nuxeoTree"></div>
		<div id="<portlet:namespace/>loader"></div>
	</div>
	
</aui:form>

<script type="text/javascript">

	function <portlet:namespace/>checkTargetSelection() {
		var sendTo = document.getElementById('<portlet:namespace/>'+'exportTargetSendTo').checked;
		if(sendTo)
			document.getElementById('<portlet:namespace/>'+'targetChooser').style.display = 'block';
		else
			document.getElementById('<portlet:namespace/>'+'targetChooser').style.display = 'none';
	}

	function <portlet:namespace/>animationOnOff(on, A) {
		if(on)
			A.one('#<portlet:namespace/>loader').setHTML(A.Node.create('<div class="loading-animation" />'));
		else
			A.one('#<portlet:namespace/>loader').setHTML(A.Node.create('<div />'));
	}
	
	function <portlet:namespace/>callbackJsonTree(jsonResponse) {		
		var personalDirs = [];
		personalDirs.push({
			label: '/',
			leaf: true,
			type: 'radio',
			id: 'personal'
		});
		for(var i in jsonResponse.personalDir) {
			personalDirs.push({
				label: jsonResponse.personalDir[i]+'/',
				leaf: true,
				type: 'radio',
				id: 'personal_'+jsonResponse.personalDir[i]
			});
		}
		
		var publicDirs = [];		
		for(var i in jsonResponse.publicDir) {
			publicDirs.push({
				label: jsonResponse.publicDir[i]+'/',
				leaf: true,
				type: 'radio',
				id: 'public_'+jsonResponse.publicDir[i]
			});
		}
		
		<portlet:namespace/>showTree(
			[
				{	children: personalDirs,
					id: 'personal_none',
					expanded: true,
					label: 'Personal Workspace'
				}, {
					children: publicDirs,
					id: 'public_none',
					expanded: false,
					label: 'Public Workspaces'
				}
			]
		);
				
	}
	
	function <portlet:namespace/>showTree(folderChildren) {
		AUI().use('aui-tree-view', function(A) {

			    /* Test:
			    var children = [
			      {
			        children: [
						{label: '/', leaf: true, type: 'radio', id: 'personal'},     
						{label: '/Watermelon', leaf: true, type: 'radio', id: 'personal_watermelon'},
						{label: '/Apricot', leaf: true, type: 'radio'},
						{label: '/Pineapple', leaf: true, type: 'radio'}
			        ],
			        id: 'personal_none',
			        expanded: true,
			        label: 'Personal Workspace',
			      },
			      {
			        children: [
						{label: 'Watermelon', leaf: true, type: 'radio'},
						{label: 'Apricot', leaf: true, type: 'radio'},
						{label: 'Pineapple', leaf: true, type: 'radio'}
			        ],
			        id: 'public_none',
			        expanded: false,
			        label: 'Public Workspaces'
			      }
			    ];
			    */
	
			    // Create a TreeView Component
			    var tree = new A.TreeView(
			      {
			        boundingBox: '#<portlet:namespace/>nuxeoTree',
			        children: folderChildren,
			        // https://www.liferay.com/de/community/forums/-/message_boards/message/10650173
			        after: { lastSelectedChange: function(e){ <portlet:namespace/>inputSelectedDir(e.newVal.get('id')); }}
			      }
			    ).render();
			    
			    <portlet:namespace/>animationOnOff(false, A);
		});
	}
	
	function <portlet:namespace/>inputSelectedDir(id) {
		console.log('id: '+id);
		document.getElementById('<portlet:namespace/>'+'chosenDir').value = id;
	}
	
	function <portlet:namespace/>initDirTree() {
	    AUI().use('aui-io-request', function(A){
	    	
	    	<portlet:namespace/>animationOnOff(true, A);
	    	
	        A.io.request('${ajaxResourceUrl}', {
	               method: 'post',
	               data: {},
	               on: {
	                   success: function() {
	                       <portlet:namespace/>callbackJsonTree(JSON.parse(this.get('responseData')));
	                    },
						failure: function() {
							<portlet:namespace/>animationOnOff(false, A);
						}
	              }
	        });
	    });
	}
	
	<portlet:namespace/>initDirTree();
</script>