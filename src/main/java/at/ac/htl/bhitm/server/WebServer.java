package at.ac.htl.bhitm.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import at.ac.htl.bhitm.backend.item.*;
import at.ac.htl.bhitm.backend.user.*;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;



@Path("/")
public class WebServer {
    private ItemsResource mng = new ItemsResource();
    private ItemFactory factory = new ItemFactory();
    UsersResource userRepository = new UsersResource();
    private User user;

    @Inject
    @Location("login/index.html")
    Template loginTemplate;

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance login(@QueryParam("username") String username, @QueryParam("message") String message, @Context HttpServletRequest request) {
        
        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");
        if (user != null) {
            try {
                loginUser(username, request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        
        if (username == null) {
            return loginTemplate.data("username", "")
                    .data("message", message != null ? message : "");
        } else if (user != null) {
            return overview(username, request);
        }

        return loginTemplate.data("username", username)
                .data("message", "User not found");
    }

    @POST
    @Path("/loginUser")
    public Response loginUser(@FormParam("username") String username, @Context HttpServletRequest request) throws URISyntaxException {
        User user = userRepository.login(username);

        HttpSession session = request.getSession();
        session.setAttribute("user", user);

        if (user != null) {
            return Response.seeOther(new URI("/overview")).build();
        } else {
            return Response.seeOther(new URI("/login?message=Login%20failed")).build();
        }
    }

    @Inject
    @Location("register/index.html")
    Template registerTemplate;

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance register() {

        return registerTemplate.data("username", null);
    }

    @POST
    @Path("/registerUser")
    @Transactional
    public Response registerUser(@FormParam("username") String username, @FormParam("firstname") String firstname, @FormParam("lastname") String lastname) throws URISyntaxException {
    
        userRepository.register(new User(firstname, lastname, username));
    
        return Response.seeOther(new URI("/login")).build();
    }


    @Inject
    @Location("overview/index.html")
    Template overviewTemplate;

    @GET
    @Path("/overview")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance overview(@QueryParam("filter") String filter, @Context HttpServletRequest request) {
        
        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");
        
        List<Item> filteredItems;
        if ("LOST".equals(filter)) {
            filteredItems = mng.all().stream()
                .filter(item -> filter.equals(item.getCurrentStatus().toString()))
                .collect(Collectors.toList());
        } else if ("FOUND".equals(filter)) {
            filteredItems = mng.all().stream()
                .filter(item -> filter.equals(item.getCurrentStatus().toString()))
                .collect(Collectors.toList());
        } else {
            filteredItems = new ArrayList<>(mng.all());
            filteredItems.sort((item1, item2) -> item1.getDate().compareTo(item2.getDate()));
        }

        return overviewTemplate.data("filteredItems", filteredItems)
        .data("filter", filter)
        .data("user", user);
        }

    @Inject
    @Location("details/index.html")
    Template detailsTemplate;

    @GET
    @Path("/details")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance details(@QueryParam("index") Integer index, @Context HttpServletRequest request){

        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");

        Item item = null;
        String lostOrFound = "";
        if (index != null) {
            item = mng.getById((long) index);
            lostOrFound = item.getCurrentStatus().toString().equals("LOST") ? "Verlust" : "Fund";
        }

        return detailsTemplate.data("item", item)
        .data("prefix", lostOrFound)
                .data("user", user);
    }                     

    @Inject
    @Location("report/index.html")
    Template reportTemplate;

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance report(@QueryParam("i") String line, @Context HttpServletRequest request) {

        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");

        String message = null;

        if (user == null) {
            return loginTemplate.data("username", "")
                    .data("message", "Login to Report an Item");
        }

        if (line != null) {
            try {
                if (line.split(";").length < 4) {
                    line += "null";
                }
                Item item = factory.createFromString(line);
                
                item.setOwner(user);

                mng.add(item);

                message = "Reported Item successfully";
            } catch (Exception e) {
                message = "Reported Item failed" + e.getMessage();
            }
        }
        return reportTemplate.data("line", line)
                             .data("mng", mng)
                             .data("message", message);
    }
 
    @Inject
    @Location("edit/index.html")
    Template editTemplate;

    @GET
    @Path("/edit")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@QueryParam("index") Integer index, 
    @QueryParam("title") String title, 
    @QueryParam("desc") String description, 
    @QueryParam("imgPath") String imgPath, 
    @QueryParam("status") ItemLevel status, 
    @Context HttpServletRequest request){
        
        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");
        
        if (user != null) {
            if (index == null) {
                return details(index, request);
            }
        } else {
            return details(index, request);
        }

        Item item = mng.getById((long) index);
        if (title != null && imgPath != null) {
            mng.edit(item.getId(), new Item(status, title, description, imgPath));
        }

        String lostOrFound = "";
        if (index != null) {
            lostOrFound = item.getCurrentStatus().toString().equals("LOST") ? "Verlust" : "Fund";
        }

        return editTemplate.data("item", item)
        .data("prefix", lostOrFound)
        .data("user", user);    
    }

    @Inject
    @Location("table/index.html")
    Template tableTemplate;

    @GET
    @Path("/table")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance table(@QueryParam("index") Integer index, @QueryParam("title") String title, @QueryParam("desc") String description, @QueryParam("imgPath") String imgPath, @QueryParam("status") ItemLevel status, @Context HttpServletRequest request){
        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");

        List<Item> items = new ArrayList<>(mng.all());

        return tableTemplate.data("items", items)
        .data("user", user);      
    }

    @Inject
    @Location("profile/index.html")
    Template userTemplate;

    @GET
    @Path("/profile")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile(@QueryParam("index") Integer index, @Context HttpServletRequest request){
        HttpSession session = request.getSession();
        user = (User) session.getAttribute("user");

        if(index == null) {
            throw new Error("No User Id in path (missing Index)!");
        }

        LinkedList<Item> neededItems = new LinkedList<>();

        User currentUser = userRepository.getById((long) index);
        for (Item item : mng.all()) {
            if (item.getOwner().equals(currentUser)) {
                neededItems.add(item);
            }
        }

        return userTemplate.data("currentUser", currentUser)
        .data("items", neededItems)
        .data("user", user);   
    }

    @Inject
    Template navbar;

    @GET
    @Path("/navbar")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getNavbar(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        return navbar.data("userId", user.getId());
    }
}