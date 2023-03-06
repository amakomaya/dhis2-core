/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.program;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */

@JacksonXmlRootElement( localName = "programTrackedEntityAttributeGroup", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramTrackedEntityAttributeGroup
    extends BaseNameableObject implements MetadataObject
{
    private Set<ProgramTrackedEntityAttribute> attributes = new HashSet<>();

    private UniqunessType uniqunessType;

    private String description;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramTrackedEntityAttributeGroup()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addAttribute( ProgramTrackedEntityAttribute attribute )
    {
        attributes.add( attribute );
        attribute.getGroups().add( this );
    }

    public void removeAttribute( ProgramTrackedEntityAttribute attribute )
    {
        attributes.remove( attribute );
        attribute.getGroups().remove( this );
    }

    public void removeAllAttributes()
    {
        for ( ProgramTrackedEntityAttribute attribute : attributes )
        {
            attribute.getGroups().remove( this );
        }

        attributes.clear();
    }

    public void updateAttributes( Set<ProgramTrackedEntityAttribute> updates )
    {
        for ( ProgramTrackedEntityAttribute attribute : new HashSet<>( attributes ) )
        {
            if ( !updates.contains( attribute ) )
            {
                removeAttribute( attribute );
            }
        }

        for ( ProgramTrackedEntityAttribute attribute : updates )
        {
            addAttribute( attribute );
        }
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty( value = "attributes" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "attributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attribute", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramTrackedEntityAttribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( Set<ProgramTrackedEntityAttribute> attributes )
    {
        this.attributes = attributes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UniqunessType getUniqunessType()
    {
        return uniqunessType;
    }

    public void setUniqunessType( UniqunessType uniqunessType )
    {
        this.uniqunessType = uniqunessType;
    }
}