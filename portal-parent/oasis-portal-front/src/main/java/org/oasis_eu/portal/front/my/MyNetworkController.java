package org.oasis_eu.portal.front.my;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.oasis_eu.portal.front.generic.PortalController;
import org.oasis_eu.portal.model.MyNavigation;
import org.oasis_eu.portal.model.appsmanagement.Authority;
import org.oasis_eu.portal.services.MyNavigationService;
import org.oasis_eu.portal.services.NetworkService;
import org.oasis_eu.spring.kernel.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 
 * @author mkalamalami
 *
 */
@Controller
@RequestMapping("/my/network")
public class MyNetworkController extends PortalController {

	private static final Logger logger = LoggerFactory.getLogger(MyNetworkController.class);

    @Autowired
    private MyNavigationService myNavigationService;

    @Autowired
    private NetworkService networkService;


    @ModelAttribute("navigation")
    private List<MyNavigation> getNavigation() {
        return myNavigationService.getNavigation("network");
    }

    @RequestMapping(method = RequestMethod.GET, value="")
    public String network(Model model) throws ExecutionException {
    	initModel(model);
        return "my-network";
    }

    @RequestMapping(method = RequestMethod.GET, value="/fragment/{fragmentId}")
    public String getFragment(@PathVariable("fragmentId") String fragmentId, Model model) throws ExecutionException {
    	initModel(model);
        return "my-network :: " + fragmentId;
    }

    @RequestMapping(method = RequestMethod.POST, value="/api/agent")
    public String saveAgentStatus(@RequestBody AgentStatusRequest request) {
        networkService.updateAgentStatus(request.agentId, request.organizationId, request.admin);
        return "redirect:/my/network/fragment/organizations";
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/api/agent/{agentId}/{organizationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAgent(@PathVariable String agentId, @PathVariable String organizationId) {
        networkService.removeAgentFromOrganization(agentId, organizationId);
    }

	private void initModel(Model model) throws ExecutionException {

        List<Authority> authorities = networkService.getMyAuthorities(false);
        model.addAttribute("authorities", authorities);
        model.addAttribute("agents", networkService.getAgents(authorities));
    }




    @RequestMapping(value = "/api/remove-message/{agentId}/{organizationId}", method = RequestMethod.GET)
    @ResponseBody
    public String getRemoveMessage(@PathVariable String agentId, @PathVariable String organizationId) {
        return networkService.getRemoveMessage(agentId, organizationId);
    }


    @RequestMapping(value = "/api/invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invite(@RequestBody InvitationRequest request) {
        networkService.invite(request.email, request.organizationId);
    }


    public static class AgentStatusRequest {
        @JsonProperty("agentid") String agentId;
        @JsonProperty("orgid") String organizationId;
        @JsonProperty("admin") boolean admin;
    }

    public static class InvitationRequest {
        @JsonProperty("orgid") String organizationId;
        @JsonProperty("email") String email;
    }

}