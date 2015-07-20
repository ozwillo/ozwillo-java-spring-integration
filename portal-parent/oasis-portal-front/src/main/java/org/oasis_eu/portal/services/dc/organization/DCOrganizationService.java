package org.oasis_eu.portal.services.dc.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.oasis_eu.portal.services.PortalSystemUserService;
import org.oasis_eu.spring.datacore.DatacoreClient;
import org.oasis_eu.spring.datacore.model.DCOperator;
import org.oasis_eu.spring.datacore.model.DCQueryParameters;
import org.oasis_eu.spring.datacore.model.DCResource;
import org.oasis_eu.spring.datacore.model.DCResult;
import org.oasis_eu.spring.datacore.model.DCRights;
import org.oasis_eu.spring.kernel.model.DCOrganizationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * User: Ignacio
 * Date: 7/2/15
 */

@Service
public class DCOrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(DCOrganizationService.class);

    @Autowired
    private DatacoreClient datacore;
    @Autowired
    private PortalSystemUserService portalSystemUserService;

    @Value("${application.dcOrg.project: org_0}")
    private String dcOrgProjectName;// = "org_0";

    @Value("${application.dcOrg.orgModel: org:Organization_0}")
    private String dcOrgModel;// = "org:Organization_0";

    @Value("${application.dcOrg.baseUri: http://data.ozwillo.com/dc/type}")
    private String dcBaseUri;// = "http://data.ozwillo.com/dc/type";


    public DCOrganization searchOrganization(String lang, String country, String sector, String legalName, String regNumber) {

        DCOrganization dcOrganization = new DCOrganization();

        DCResource resource = fetchDCOrganizationResource(lang, country, sector, legalName, regNumber);
        if(resource != null )
            dcOrganization = toDCOrganization(resource,lang);
        else{
            dcOrganization = new DCOrganization();
        }

        return dcOrganization;
    }

    private DCResource fetchDCOrganizationResource(String lang, String country, String sector, String legalName, String regNumber) {
        DCQueryParameters params = new DCQueryParameters()
                      .and("org:sector", DCOperator.EQ, sector) 
                      .and("org:legalName.v", DCOperator.EQ, DCOperator.REGEX.getRepresentation()+legalName)
                      .and("org:regNumber", DCOperator.EQ, regNumber)
                      .and("org:country", DCOperator.EQ, country);

        logger.debug("Querying the Data Core");
        long queryStart = System.currentTimeMillis();
        List<DCResource> resources = datacore.findResources(dcOrgProjectName.trim(), dcOrgModel.trim(), params, 0, 1);
        /*if(resources ==null || resources.isEmpty()){
            //TODO this is for test only. If is not found using all search factors, it re-search only by regNum
            resources = datacore.findResources(dcOrgProjectName.trim(), dcOrgModel.trim(), new DCQueryParameters("org:regNumber", DCOperator.EQ, regNumber), 0, 1);
        }*/
        long queryEnd = System.currentTimeMillis();
        logger.debug("Fetched {} resources in {} ms", resources.size(), queryEnd-queryStart);

        return resources.isEmpty()? null : resources.get(0);
    }

    public DCResource create(DCOrganization dcOrganization){
        // re-get DC resource before creation to validate that it doesn't exist
        DCResource dcResource = fetchDCOrganizationResource(dcOrganization.getLang(),dcOrganization.getCountry(),
                DCOrganizationType.getDCOrganizationType(dcOrganization.getSector_type()).name(),
                dcOrganization.getLegal_name(),dcOrganization.getTax_reg_num());
        // if found check that version hasn't changed since filling the form (i.e. since clicking on "search"),
        if (dcResource != null && dcResource.getVersion() == Integer.parseInt(dcOrganization.getVersion()) ){ //found in DC
            // there are no previous updates, merge it from form fields and do datacoreClient.saveResource()
            mergeDCOrgToDCResources(dcOrganization, dcResource);
            datacore.updateResource(dcOrgProjectName.trim(), dcResource);
            return dcResource;
        }else if (dcResource == null || dcResource.isNew()){  // still doesn't exist in DC
            DCResult newCreatedDCRes =  datacore.saveResource(dcOrgProjectName.trim(), toNewDCResource(dcOrganization));
            if(newCreatedDCRes.getResource() != null){ 
                dcOrganization.setId(newCreatedDCRes.getResource().getUri());
                return newCreatedDCRes.getResource();
            } 
        }
        
        // if version has changed : "Sorry, did change while you were editing it, please copy your fields, close and restart the wizard"
        return null;
    }

    /** Update DC Organization data re-using this.create(DCOrganization) method. */
    public DCResource update(DCOrganization dcOrganization){
        return this.create(dcOrganization);
    }

    /** Change rights of DC Organization. */
    public boolean changeDCOrganizationRights(DCResource dcResource,String kOrgId){
        DCRights rights = new DCRights();
        rights.setOwners(new ImmutableList.Builder<String>().add(kOrgId).build() );

        //get admin authentication and change organization rights 
        portalSystemUserService.runAs(new Runnable() { 
            //Inner class with Runnable, its used as a function parameter (executed at parameter declaration)
            @Override
            public void run() {
                datacore.setRightsOnResource(dcOrgProjectName, dcResource, rights).getResource();
            }
        });
        return true;
    }


    // Helper & Handler methods

    private DCResource mergeDCOrgToDCResources(DCOrganization fromOrg, DCResource toRes){
        // Organization data
        toRes.setMappedList("org:legalName", valueAsDCList(fromOrg.getLegal_name(), fromOrg.getLang())); //list

        DCOrganizationType dcOrganizationType = DCOrganizationType.getDCOrganizationType(fromOrg.getSector_type());
        toRes.set("org:sector", (dcOrganizationType != null) ? dcOrganizationType.name() : "");

        toRes.set("org:status", fromOrg.isIn_activity() ? "Enabled" : "Disabled");
        toRes.setMappedList("org:altName", valueAsDCList(fromOrg.getAlt_name(), fromOrg.getLang())); //list
        toRes.set("org:type", fromOrg.getOrg_type());
        toRes.set("org:regNumber", fromOrg.getTax_reg_num());
        toRes.set("orgpu:officialId", fromOrg.getTax_reg_official_id()); /* Only for Public organizations*/
        toRes.set("org:activity", fromOrg.getTax_reg_activity_uri());
        if(dcOrganizationType.equals(DCOrganizationType.Public)){
            toRes.set("orgpu:jurisdiction", fromOrg.getJurisdiction_uri()); /* Only for Public organizations*/
        }
        toRes.set("org:phoneNumber", fromOrg.getPhone_number());
        toRes.set("org:webSite", fromOrg.getWeb_site());
        toRes.set("org:email", fromOrg.getEmail());
        // Geolocation data
        toRes.set("org:streetAndNumber", fromOrg.getStreet_and_number());
        toRes.set("org:supField", fromOrg.getAdditional_address_field());
        toRes.set("org:POBox", fromOrg.getPo_box());
        toRes.set("org:postName", fromOrg.getCity_uri());
        toRes.set("org:postCode", fromOrg.getZip());
        toRes.set("org:cedex", fromOrg.getCedex());
        toRes.set("org:country", fromOrg.getCountry_uri());

        //toRes.set("org:latitude", fromOrg.getLatitude());   //use once mapping localization is ready
        //toRes.set("org:longitude", fromOrg.getLongitude()); //use once mapping localization is ready

        //toRes.setLastModified(ZonedDateTime.now().toInstant());

        return toRes; 
    }
    private List<Map<String, DCResource.Value>> valueAsDCList(String value, String language){
        Map<String, DCResource.Value> myMap = new HashMap<>(2);
        myMap.put("@language", new DCResource.StringValue(language));
        myMap.put("@value", new DCResource.StringValue(value));

        List<Map<String, DCResource.Value>> legalNameLst = new ArrayList<>(1);
        legalNameLst.add(myMap);
        return legalNameLst;
    }
    private DCResource toNewDCResource(DCOrganization dcOrganization){
        DCResource dcResource = new DCResource();
        mergeDCOrgToDCResources(dcOrganization, dcResource);

        dcResource = setDCIdOrganization(dcResource, dcOrganization.getSector_type(), dcOrganization.getLang(), dcOrganization.getTax_reg_num());

        return dcResource;
    }

    private DCResource setDCIdOrganization(DCResource dcResource, String type, @NotNull String lang, String regNumber){
        //"@id" : "http://data.ozwillo.com/dc/type/orgprfr:OrgPriv%C3%A9e_0/FR/47952557800049",
        String px = DCOrganizationType.getDCOrganizationType(type).equals(DCOrganizationType.Private) ? "pr": "pu";
        String cx = lang.toLowerCase();

        String orgModelPrefix = "org"+px+cx;
        String orgModelSuffix = dcOrgPrefixToSuffix.get(orgModelPrefix);
        String orgModelType = orgModelPrefix + ":" + orgModelSuffix + "_0";

        dcResource.setBaseUri(dcBaseUri.trim());
        dcResource.setType(orgModelType);
        dcResource.setIri(cx.toUpperCase()+"/"+regNumber);
        return dcResource;
    }

    private static final Map<String, String> dcOrgPrefixToSuffix = new ImmutableMap.Builder<String, String>()
            //private
            .put("orgpr",   "PrivateOrg")
            .put("orgprfr", "OrgPrivée")
            .put("orgprbg", "ЧастнаОрг")
            .put("orgprit", "OrgPrivata")
            .put("orgprtr", "ÖzelSektKuru")
            .put("orgpres", "OrgPrivada")
            //public
            .put("orgpu",   "PublicOrg")
            .put("orgpufr", "OrgPublique")
            .put("orgpubg", "ПубличнаОрг")
            .put("orgpuit", "OrgPubblica")
            .put("orgputr", "KamuKurumu")
            .put("orgpues", "OrgPública")
            .build();
    
    public DCOrganization toDCOrganization(DCResource res, String language) {

        String legalName =       getBestI18nValue(res, language, "org:legalName", null); //Mapped list
        boolean in_activity =    getBestI18nValue(res, language, "org:status", null).equalsIgnoreCase("enabled") ? true : false;
        String sector =          getBestI18nValue(res, language, "org:sector", null);
        String altName =         getBestI18nValue(res, language, "org:altName", null); //Mapped list

        String taxRegAct_uri =    getBestI18nValue(res, language, "org:activity", null);
        String taxRegAct =       taxRegAct_uri == null ? null : getBestI18nValue(
                                       datacore.getResourceFromURI(dcOrgProjectName, taxRegAct_uri).getResource(), language, "orgact:code", null
                                    );
        String officialId =      getBestI18nValue(res, language, "orgpu:officialId", null);
        String regNumber =       getBestI18nValue(res, language, "org:regNumber", null);

        String jurisdiction_uri =  getBestI18nValue(res, language, "orgpu:jurisdiction", null);

        String phoneNumber =     getBestI18nValue(res, language, "org:phoneNumber", null);
        String webSite =         getBestI18nValue(res, language, "org:webSite", null);
        String email =           getBestI18nValue(res, language, "org:email", null);
        // Geolocation data
        String streetAndNumber = getBestI18nValue(res, language, "org:streetAndNumber", null);
        String supField =        getBestI18nValue(res, language, "org:supField", null);
        String POBox =           getBestI18nValue(res, language, "org:POBox", null);
        String city_uri =        getBestI18nValue(res, language, "org:postName", null);
        String zip =             getBestI18nValue(res, language, "adrpost:postCode", "org:postCode");
        String cedex =           getBestI18nValue(res, language, "org:cedex", null);

        String country_uri =      getBestI18nValue(res, language, "org:country", null);
        String country =         country_uri == null ? null : getBestI18nValue(
                                       datacore.getResourceFromURI(dcOrgProjectName, country_uri).getResource(), language, "geoco:name", null
                                    );

        //String longitude=     getBestI18nValue(res, "org:longitude", null);
        //String latitude =     getBestI18nValue(res, "org:latitude", null);

        DCOrganization dcOrg = new DCOrganization();
        dcOrg.setLegal_name(legalName);
        dcOrg.setIn_activity(in_activity);
        dcOrg.setSector_type(sector);
        dcOrg.setAlt_name(altName);

        dcOrg.setTax_reg_activity_uri(taxRegAct_uri); dcOrg.setTax_reg_activity(taxRegAct);
        dcOrg.setTax_reg_num(regNumber);
        dcOrg.setTax_reg_official_id(officialId); /* Only for Public organizations*/

        dcOrg.setJurisdiction_uri(jurisdiction_uri); /* Only for Public organizations*/

        dcOrg.setPhone_number(phoneNumber);
        dcOrg.setWeb_site(webSite);
        dcOrg.setEmail(email);
        
        dcOrg.setStreet_and_number(streetAndNumber);
        dcOrg.setAdditional_address_field(supField);
        dcOrg.setPo_box(POBox);
        dcOrg.setCedex(cedex);
        dcOrg.setCity_uri(city_uri);
        dcOrg.setZip(zip);
        dcOrg.setCountry_uri(country_uri); dcOrg.setCountry(country);

        dcOrg.setId(res.getUri());
        dcOrg.setExist(true); // Organization was found !
        dcOrg.setLang(language);
        dcOrg.setVersion(res.getVersion()+"");

        return dcOrg;
    }

    /**
     * Return the resource value, first matching the fieldName, if not found then match with the altFieldName.
     * In case a Listed Map is found, the inner values are matched using i18n key @language, and @value. 
     * @param resource
     * @param language
     * @param fieldName
     * @param altFieldName
     * @return String with found value, null if not value was found (empty counts as not found value)
     */
    @SuppressWarnings("unchecked")
    private String getBestI18nValue(DCResource resource, String language, String fieldName, String altFieldName){
        if(resource == null){return null;}
        Object object = resource.get(fieldName);
        if( object == null && (altFieldName != null && !altFieldName.isEmpty() ) ){
            logger.warn("Field \"{}\" not found. Fallback using field name \"{}\"", fieldName, altFieldName);
            object = resource.get(altFieldName);
        }
        if (object == null) { // if after double matched is not found then return null
            logger.warn("DC Resource {} of type {} has no field required fields.", resource.getUri(), resource.getType());
            return null;
        }
        // Parse the list checking if it's a simple list or a listed map
        if (object instanceof List ) {
            String valueMap = null;
            for (Object obj: (List<Object>) object) {
                if (obj instanceof Map) {
                    Map<String, String> nameMap = (Map<String, String>) obj;
                    logger.debug("nameMap: " + nameMap.toString());
                    String l = nameMap.get("@language"); // TODO Q why ?? @language only in application/json+ld, otherwise l
                    if (l == null) { continue; /* shouldn't happen */ }
                    if (l.equals(language)) {
                        String val = nameMap.get("@value");
                        return val == null || val.isEmpty() ? null : nameMap.get("@value"); //break; // can't find better
                    }
                    if (valueMap == null) { // takes the last valid match
                        valueMap = nameMap.get("@value"); // TODO Q why ?? @value only in application/json+ld, otherwise v
                    }
                }else {valueMap = ((List<String>)object).toString();} // Its a list of strings //TODO use it and test it
            }
            return valueMap;

        }else if (object instanceof String ) {
            String val = (String)object;
            return val == null || val.isEmpty() ? null : val;
        }
        return null;
    }


}
