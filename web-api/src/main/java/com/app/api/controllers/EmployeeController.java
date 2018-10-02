package com.app.api.controllers;

import com.app.api.BaseController;
import com.app.dao.CustomerDao;
import com.app.dao.EmployeeDao;
import com.app.model.BaseResponse;
import com.app.model.customer.CustomerModel;
import com.app.model.employee.EmployeeModel;
import com.app.model.employee.EmployeeResponse;
import com.app.util.HibernateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.annotation.security.RolesAllowed;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;


@Path("employees")
@Api(value = "Employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeController extends BaseController {

    @GET
    @ApiOperation(value = "Get list of employees", response = EmployeeResponse.class)
    @RolesAllowed({"ADMIN"})
    public Response getEmployeeList(
        @ApiParam(value="Employee Id") @QueryParam("id") int id,
        @ApiParam(value="Department")  @QueryParam("department") String dept,
        @ApiParam(value="Use % for wildcard like 'Steav%' ")    @QueryParam("first-name") String firstName,
        @ApiParam(value="Page No, Starts from 1 ", example="1") @DefaultValue("1")  @QueryParam("page")  int page,
        @ApiParam(value="Items in each page", example="20")     @DefaultValue("20") @QueryParam("page-size") int pageSize
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


    @POST
    @ApiOperation(value = "Add a employee", response = BaseResponse.class)
    @RolesAllowed({"ADMIN"})
    public Response addEmployee(EmployeeModel emp) {

        BaseResponse resp = new BaseResponse();
        Session hbrSession = HibernateUtil.getSession();
        hbrSession.setFlushMode(FlushMode.ALWAYS);
        try {
            hbrSession.beginTransaction();
            hbrSession.save(emp);
            hbrSession.getTransaction().commit();
        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot add employee - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
        }

        return Response.ok(resp).build();
    }

    @PUT
    @ApiOperation(value = "Update a Employee", response = BaseResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response updateEmployee(EmployeeModel emp) {

        BaseResponse resp = new BaseResponse();
        Session hbrSession = HibernateUtil.getSession();
        hbrSession.setFlushMode(FlushMode.ALWAYS);
        try {
            EmployeeModel foundEmp  = EmployeeDao.getById(hbrSession, emp.getId());
            if (foundEmp != null){
                hbrSession.beginTransaction();
                hbrSession.merge(emp);
                hbrSession.getTransaction().commit();
                resp.setSuccessMessage(String.format("Employee Updated (id:%s)", emp.getId()));
                return Response.ok(resp).build();
            }
            else{
                resp.setErrorMessage(String.format("Cannot Update - Employee not found (id:%s)", emp.getId()));
                return Response.ok(resp).build();
            }
        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot update Employee - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
            return Response.ok(resp).build();
        }

    }


    @DELETE
    @Path("{employeeId}")
    @ApiOperation(value = "Delete a Employee", response = BaseResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response deleteEmployee(@ApiParam(value="Employee Id", example="1") @PathParam("employeeId") Integer employeeId) {

        BaseResponse resp = new BaseResponse();
        Session hbrSession = HibernateUtil.getSession();
        hbrSession.setFlushMode(FlushMode.ALWAYS);
        try {
            BigDecimal referenceCount = CustomerDao.getReferenceCount(hbrSession, employeeId);
            if (referenceCount.intValue() > 0){
                resp.setErrorMessage("Cannot delete Employee, Referenced in other tables");
                return Response.ok(resp).build();
            }
            else {
                CustomerModel foundCust  = CustomerDao.getById(hbrSession, employeeId);
                if (foundCust==null){
                    resp.setErrorMessage(String.format("Cannot delete - Employee do not exist (id:%s)", employeeId));
                    return Response.ok(resp).build();
                }
                else{
                    hbrSession.beginTransaction();
                    CustomerDao.delete(hbrSession, employeeId);
                    hbrSession.getTransaction().commit();
                    resp.setSuccessMessage(String.format("Employee deleted (id:%s)", employeeId));
                    return Response.ok(resp).build();

                }
            }

        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot delete Customer - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
            return Response.ok(resp).build();
        }
    }


}
