<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:oryx="http://oryx-editor.org/">

	<xsl:output method="xml" />
	
	<xsl:template match="rdf:Description">	
		<xsl:variable name="typeString" select="./oryx:type" />	
		<xsl:variable name="type">
			<xsl:call-template name="get-exact-type">
				<xsl:with-param name="typeString" select="$typeString" />
			</xsl:call-template>
		</xsl:variable>

		<xsl:if test="$type='worksheet'">
			<!-- root element -->
			<topology>
				<xsl:variable name="name" select="./oryx:name" />
				<xsl:if test="$name!=''">
					<xsl:attribute name="name">
						<xsl:value-of select="concat($name,'topology') " />
					</xsl:attribute>
				</xsl:if>
				
				<xsl:variable name="targetNamespace" select="./oryx:targetnamespace" />
				<xsl:if test="$targetNamespace!=''">
					<xsl:attribute name="targetNamespace">
						<xsl:value-of select="$targetNamespace" />
					</xsl:attribute>
				</xsl:if>
				
				<xsl:call-template name="add-otherxmlns-attribute"/>
					
				<participantType>
					<xsl:call-template name="find-all-participantTypes"/>
				</participantType>	
				
				<participants>
					<xsl:call-template name="find-all-participants"/>
				</participants>	
				
				<messageLinks>
					<xsl:call-template name="find-all-messageLinks"/>
				</messageLinks>	
			</topology>	
	 	</xsl:if>
	</xsl:template>
	
	<xsl:template name="find-all-participants">
        <xsl:for-each select="//rdf:Description">
			<xsl:variable name="typeString" select="./oryx:type" />	
			<xsl:variable name="type">
				<xsl:call-template name="get-exact-type">
					<xsl:with-param name="typeString" select="$typeString" />
				</xsl:call-template>
			</xsl:variable>
			
			<!--process-->
			<xsl:if test="$type='process'">
				<participant>
					<xsl:variable name="name" select="./oryx:name" />
					<xsl:if test="$name!=''">
						<xsl:attribute name="name">
							<xsl:value-of select="$name" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="type" select="./oryx:type" />
					<xsl:if test="$type!=''">
						<xsl:attribute name="type">
							<xsl:value-of select="$type" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="selects" select="./oryx:selects" />
					<xsl:if test="$selects!=''">
						<xsl:attribute name="selects">
							<xsl:value-of select="$selects" />
						</xsl:attribute>
					</xsl:if>
				</participant>
			</xsl:if>	
		</xsl:for-each>
	</xsl:template>	
	
	<xsl:template name="find-all-participantTypes">
        <xsl:for-each select="//rdf:Description">
			<xsl:variable name="typeString" select="./oryx:type" />	
			<xsl:variable name="type">
				<xsl:call-template name="get-exact-type">
					<xsl:with-param name="typeString" select="$typeString" />
				</xsl:call-template>
			</xsl:variable>
			
			<!--process-->
			<xsl:if test="$type='process'">
				<participantType>
				</participantType>
			</xsl:if>	
		</xsl:for-each>
	</xsl:template>	
	
	<xsl:template name="find-all-messageLinks">
        <xsl:for-each select="//rdf:Description">
			<xsl:variable name="typeString" select="./oryx:type" />	
			<xsl:variable name="type">
				<xsl:call-template name="get-exact-type">
					<xsl:with-param name="typeString" select="$typeString" />
				</xsl:call-template>
			</xsl:variable>
			
			<!--messageLink-->
			<xsl:if test="$type='messageLink'">
				<messageLink>
					<xsl:variable name="name" select="./oryx:name" />
					<xsl:if test="$name!=''">
						<xsl:attribute name="name">
							<xsl:value-of select="$name" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="sender" select="./oryx:sender" />
					<xsl:if test="$sender!=''">
						<xsl:attribute name="sender">
							<xsl:value-of select="$sender" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="sendActivity" select="./oryx:sendactivity" />
					<xsl:if test="$sendActivity!=''">
						<xsl:attribute name="sendActivity">
							<xsl:value-of select="$sendActivity" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="receiver" select="./oryx:receiver" />
					<xsl:if test="$receiver!=''">
						<xsl:attribute name="receiver">
							<xsl:value-of select="$receiver" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="receiveActivity" select="./oryx:receiveactivity" />
					<xsl:if test="$receiveActivity!=''">
						<xsl:attribute name="receiveActivity">
							<xsl:value-of select="$receiveActivity" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="bindSenderTo" select="./oryx:bindsenderto" />
					<xsl:if test="$bindSenderTo!=''">
						<xsl:attribute name="bindSenderTo">
							<xsl:value-of select="$bindSenderTo" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="messageName" select="./oryx:messagename" />
					<xsl:if test="$messageName!=''">
						<xsl:attribute name="messageName">
							<xsl:value-of select="$messageName" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="participantRefs" select="./oryx:participantrefs" />
					<xsl:if test="$participantRefs!=''">
						<xsl:attribute name="participantRefs">
							<xsl:value-of select="$participantRefs" />
						</xsl:attribute>
					</xsl:if>
					
					<xsl:variable name="copyParticipantRefsTo" select="./oryx:copyparticipantrefsto" />
					<xsl:if test="$copyParticipantRefsTo!=''">
						<xsl:attribute name="copyParticipantRefsTo">
							<xsl:value-of select="$copyParticipantRefsTo" />
						</xsl:attribute>
					</xsl:if>
				</messageLink>
			</xsl:if>	
		</xsl:for-each>
	</xsl:template>	
	
	<xsl:template name="add-otherxmlns-attribute">
		<xsl:variable name="otherxmlns" select="./oryx:otherxmlns" />
		<xsl:if test="$otherxmlns!=''">
			<xsl:variable name="count">
				<xsl:call-template name="get-number-of-elements-in-complex-type">
					<xsl:with-param name="original_content" select="$otherxmlns" />
				</xsl:call-template>
			</xsl:variable>
			
			<xsl:call-template name="loop-for-adding-otherxmlns-attribute">
				<xsl:with-param name="i">1</xsl:with-param>
				<xsl:with-param name="count" select="$count" />
				<xsl:with-param name="data-set" select="$otherxmlns" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="loop-for-adding-otherxmlns-attribute">
		<xsl:param name="i"/>
 		<xsl:param name="count"/>
		<xsl:param name="data-set"/>
			
 		<xsl:if test="$i &lt;= $count">
 			<xsl:variable name="prefix" select="substring-before(substring-after($data-set, 'prefix%3A%22'), '%22%2C%20namespace') " />
			<xsl:variable name="namespace" select="substring-before(substring-after($data-set, 'namespace%3A%22'), '%22%7D') " />
			<xsl:variable name="attribute-name" select="concat('xmlns:',$prefix)" />
			<xsl:attribute name="{$attribute-name}">
				<xsl:value-of select="$namespace"/>
			</xsl:attribute>
			
  			<xsl:call-template name="loop-for-adding-otherxmlns-attribute">
   				<xsl:with-param name="i" select="$i + 1"/>
   				<xsl:with-param name="count" select="$count"/>
   				<xsl:with-param name="data-set" select="substring-after($data-set,'%22%7D%2C%20%7B')"/>
  			</xsl:call-template>
 		</xsl:if>
    </xsl:template>
	
	
	<xsl:template name="get-exact-type">
		<xsl:param name="typeString" />
		<xsl:value-of select="substring-after($typeString, '#')" />
	</xsl:template>
	
	<xsl:template name="get-number-of-elements-in-complex-type">
		<xsl:param name="original_content" />
		<xsl:value-of select="substring-before(substring-after($original_content, 'totalCount%27%3A'), '%2C%20%27items') " />
	</xsl:template>	
	
	
</xsl:stylesheet>