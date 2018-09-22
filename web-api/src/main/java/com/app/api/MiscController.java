package com.app.api;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.annotation.security.PermitAll;

import com.app.DatabaseService;
import com.app.model.BaseResponse;
import io.swagger.annotations.*;
import javax.annotation.security.RolesAllowed;
import com.app.util.Constants.UserRoleConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Path("")
@Api(value = "Misc")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MiscController extends BaseController{

    @GET
    @Path("misc/version")
    @PermitAll
    @ApiOperation(value = "Return API Version (Permitted to all)")
    public Response getVersion() {
        BaseResponse resp = new BaseResponse();
        resp.setSuccessMessage("1.0.0");
        return Response.ok(resp).build();
    }

    @GET
    @Path("misc/runtime-error")
    @PermitAll
    @ApiOperation(value = "When an API encounters a runtime-error, its is captured and returned as a JSON response")
    public Response getError(
        @ApiParam(value="'true' for creating a runtime-error", example="true") @DefaultValue("true") @QueryParam("create-error") boolean createError
    )
    {
        if (createError) {
            throw new NullPointerException("The exception is generated on purpose, to demonstrate when any API encounters a runtime-error, it can be captured and sent as a JSON response, instead of sending a server generated error html page");
        }
        BaseResponse resp = new BaseResponse();
        resp.setSuccessMessage("Response when there is no runtime-error");
        return Response.ok(resp).build();
    }



    @GET
    @Path("/database-files")
    @RolesAllowed({UserRoleConstants.ROLE_ADMIN})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Zip files containing SQL scripts to fill this apps database (Example of how to send streaming response)")
    public Response downloadSqlScriptFiles() {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                String sourceDirPath="";

                try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os))) {
                    java.nio.file.Path pp = Paths.get(sourceDirPath);
                    String file1="schema.sql", file2="data.sql";

                    // Put schema.sql entry into zip
                    ZipEntry zipEntry1 = new ZipEntry(file1);
                    zos.putNextEntry(zipEntry1);
                    DatabaseService.getSqlScriptResourceAsStream(file1).transferTo(zos);
                    zos.closeEntry();

                    // Put data.sql into zip
                    ZipEntry zipEntry2 = new ZipEntry(file2);
                    zos.putNextEntry(zipEntry2);
                    DatabaseService.getSqlScriptResourceAsStream(file2).transferTo(zos);
                    zos.closeEntry();

                    zos.flush();
                    // no need to zos.close(), it will auto-close being inside try block
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new WebApplicationException("Unable to create zip file:" + e.getMessage());
                }
            }

        };

        return Response
            .ok(stream)
            .type("application/zip")
            .header("Content-Disposition", "attachment; filename=sql_script_files.zip")
            .build();
    }




}