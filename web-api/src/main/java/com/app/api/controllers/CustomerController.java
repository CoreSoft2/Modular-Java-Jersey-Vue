package com.app.api.controllers;

import com.app.api.BaseController;
import com.app.dao.CustomerDao;
import com.app.model.BaseResponse;
import com.app.model.customer.CustomerModel;
import com.app.model.customer.CustomerModel.CustomerResponse;
import com.app.model.customer.CustomerUserModel;
import com.app.model.customer.CustomerUserModel.CustomerUserResponse;
import com.app.util.HibernateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.annotation.security.RolesAllowed;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("customers")
@Api(value = "Customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController extends BaseController {

    @GET
    @ApiOperation(value = "Get list of customers", response = CustomerResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response getCustomerList(
        @ApiParam(value="Customer Id") @QueryParam("customer-id") int customerId,
        @ApiParam(value="User Id") @QueryParam("user-id") String userId,
        @ApiParam(value="Company - Use % for wildcard") @QueryParam("company") String company,
        @ApiParam(value="Search by first or last name, use % for wildcard ", example="t%")  @QueryParam("name") String name,
        @ApiParam(value="Page No, Starts from 1 ", example="1") @DefaultValue("1")  @QueryParam("page") int page,
        @ApiParam(value="Items in each page", example="20") @DefaultValue("20") @QueryParam("page-size") int pageSize
    ) {

        int recordFrom = 0;
        Criteria criteria = HibernateUtil.getSession().createCriteria(CustomerUserModel.class);

        if (customerId > 0) {
            criteria.add(Restrictions.eq("customerId", customerId));
        }
        if (StringUtils.isNotBlank(userId)) {
            criteria.add(Restrictions.eq("userId", userId));
        }
        if (StringUtils.isNotBlank(company)) {
            criteria.add(Restrictions.like("company", company).ignoreCase());
        }
        if (StringUtils.isNotBlank(name)) {
            criteria.add(
                Restrictions.or(
                    Restrictions.like("firstName",  name ).ignoreCase(),
                    Restrictions.like("lastName",  name ).ignoreCase()
                )
            );
        }
        if (page <= 0) {
            page = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 20;
        }
        recordFrom = (page - 1) * pageSize;

        // Execute the Total-Count Query first ( if main query is executed first, it results in error for count-query)
        criteria.setProjection(Projections.rowCount());
        Long rowCount = (Long)criteria.uniqueResult();

        // Execute the main query
        criteria.setProjection(null);
        criteria.setFirstResult((int) (long) recordFrom);
        criteria.setMaxResults((int) (long) pageSize);
        List<CustomerUserModel> customerUserList = criteria.list();

        CustomerUserResponse resp = new CustomerUserResponse();
        resp.setList(customerUserList);
        resp.setPageStats(rowCount.intValue(), pageSize, page,"");
        resp.setSuccessMessage("List of customers");
        return Response.ok(resp).build();
    }

    @POST
    @ApiOperation(value = "Add a Customer", response = BaseResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response addCustomer(CustomerModel cust) {

        BaseResponse resp = new BaseResponse();
        Session hbrSession = HibernateUtil.getSession();
        hbrSession.setFlushMode(FlushMode.ALWAYS);
        try {
            hbrSession.beginTransaction();
            hbrSession.save(cust);
            hbrSession.getTransaction().commit();
            resp.setSuccessMessage(String.format("Customer Added - New Customer ID : %s ", cust.getId()));
            return Response.ok(resp).build();

        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot add Customer - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
            return Response.ok(resp).build();
        }
    }

    @PUT
    @ApiOperation(value = "Update a Customer", response = BaseResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response updateCustomer(CustomerModel cust) {

        BaseResponse resp = new BaseResponse();
        Session hbrSession = HibernateUtil.getSession();
        hbrSession.setFlushMode(FlushMode.ALWAYS);
        try {
            CustomerModel foundCust  = CustomerDao.getById(hbrSession, cust.getId());
            if (foundCust != null){
                hbrSession.beginTransaction();
                hbrSession.merge(cust);
                hbrSession.getTransaction().commit();
                resp.setSuccessMessage(String.format("Customer Updated (id:%s)", cust.getId()));
                return Response.ok(resp).build();
            }
            else{
                resp.setErrorMessage(String.format("Cannot Update - Customer not found (id:%s)", cust.getId()));
                return Response.ok(resp).build();
            }
        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot update Customer - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
            return Response.ok(resp).build();
        }

    }


    @DELETE
    @Path("{customerId}")
    @ApiOperation(value = "Delete a Customer", response = BaseResponse.class)
    @RolesAllowed({"ADMIN", "SUPPORT"})
    public Response deleteCustomer(@ApiParam(value="Customer Id", example="2") @PathParam("customerId") Integer customerId) {

        BaseResponse resp = new BaseResponse();
        if (customerId == 1 ){
            resp.setErrorMessage("Customer with Id 1 is special, cannot be deleted");
            return Response.ok(resp).build();
        }

        Session hbrSession = HibernateUtil.getSession();
        try {
            CustomerModel foundCust  = CustomerDao.getById(hbrSession, customerId);
            if (foundCust==null){
                resp.setErrorMessage(String.format("Cannot delete customer - Customer do not exist (id:%s)", customerId));
                return Response.ok(resp).build();
            }

            hbrSession.beginTransaction();
            CustomerDao.delete(hbrSession, customerId);
            hbrSession.getTransaction().commit();
            resp.setSuccessMessage(String.format("Customer deleted (id:%s)", customerId));
            return Response.ok(resp).build();

        }
        catch (HibernateException | ConstraintViolationException e) {
            resp.setErrorMessage("Cannot delete Customer - " + e.getMessage() + ", " + (e.getCause()!=null? e.getCause().getMessage():""));
            return Response.ok(resp).build();
        }
    }





}
