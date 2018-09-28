import axios from 'axios';
import store from '@/store';
import RestUtil from '@/rest/RestUtil';

export default {
  login(username, password){
    
    let loginPath = RestUtil.getBasePath() + "/authenticate";

    return axios.post(loginPath+"/user", {username,password}).then(function(resp){
      if (resp.data.data){
        var respData = resp.data.data;
        console.log(respData);
        RestUtil.setToken(respData.token)
        store.commit('user'      , respData.userId )
        store.commit('userName'  , respData.fullName )
        store.commit('role'      , respData.role )
        store.commit('email'     , respData.email )
        store.commit('customerId', respData.customerId )
        store.commit('employeeId', respData.employeeId )
        store.commit('jwt'       , respData.token )
        return respData;
      }
      else{
        return resp;
      }
    })
    .catch(function(err){
      console.log("REST ERROR: %O", err.response?err.response:err);
      return Promise.reject(err);
    })
  }

}
