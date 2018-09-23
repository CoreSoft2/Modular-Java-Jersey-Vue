package com.app.api.employee;

import com.app.api.BaseController;
import com.app.model.employee.EmployeeModel;
import com.app.model.employee.EmployeeResponse;
import com.app.util.HibernateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("employees")
@Api(value = "Employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeController extends BaseController {

    @GET
    @Path("")
    @ApiOperation(value = "Get list of employees", response = EmployeeResponse.class)
    @RolesAllowed({"ADMIN"})
    public Response getEmployeeList(
        @ApiParam(value="Employee Id") @QueryParam("id") int id,
        @ApiParam(value="Department")  @QueryParam("department") String dept,
        @ApiParam(value="Use % for wildcard like 'Steav%' ")  @QueryParam("first-name") String firstName,
        @ApiParam(value="Page No, Starts from 1 ", example="1") @DefaultValue("1")  @QueryParam("page")  int page,
        @ApiParam(value="Items in each page", example="20") @DefaultValue("20") @QueryParam("page-size") int pageSize
    ) {

        int recordFrom=0;
        Criteria criteria = HibernateUtil.getSessionFactory().openSession().createCriteria(EmployeeModel.class);
        if (id > 0){
            criteria.add(Restrictions.eq("id",  id ));
        }
        if (StringUtils.isNotBlank(dept)){
            criteria.add(Restrictions.like("department",  dept ));
        }
        if (StringUtils.isNotBlank(firstName)){
            criteria.add(Restrictions.like("firstName",  firstName ));
        }
        if (page<=0){
            page = 1;
        }
        if (pageSize<=0 || pageSize > 1000){
            pageSize =20;
        }
        recordFrom = (page-1) * pageSize;

        // Execute the Hibernate Query
        criteria.setFirstResult(recordFrom);
        criteria.setMaxResults(pageSize);
        List<EmployeeModel> empList = criteria.list();
        criteria.setProjection(Projections.rowCount());
        int totalRows = Math.toIntExact((Long) criteria.uniqueResult());

        EmployeeResponse resp = new EmployeeResponse();
        resp.setList(empList);
        resp.setPageStats(totalRows, pageSize, page,"");
        resp.setSuccessMessage("List of employees");
        return Response.ok(resp).build();
    }
}
