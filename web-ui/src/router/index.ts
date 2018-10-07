import Vue from 'vue'
import Router from 'vue-router'
import { RouterOptions, Location, RouteConfig, Route } from 'vue-router'
import store from '@/store/index'

import AppShell from '@/components/app-shell/AppShell.vue'
import TopNav from '@/menu/TopNav'

//login
import LoginPage from '@/views/LoginPage.vue'

//User and Registration
import User from '@/views/User.vue'
import Register from '@/views/Register.vue'

//Charts
import Dashboard from '@/views/Dashboard.vue'

//My Cart
import MyCart from '@/views/MyCart.vue'

//Order
import Order from '@/views/Order.vue'

//Product
import Product from '@/views/Product.vue'

//Employees
import Employee from '@/views/Employee.vue'

//Customer
import Customer from '@/views/Customer.vue'


Vue.use(Router)

const router =  new Router({
  routes: [
    { path: '/',
      redirect: function(to){
        // if authenticated redirect to appshell else to login
        return '/login';
      }
    },
    { path: '/login'      , component: LoginPage , meta: { permitAll: true} },
    { path: '/register'   , component: Register  , meta: { permitAll: true} },
    { path: '/home'       , component: AppShell,
      children: [
        { path: 'my-orders'  , component: Order },
        { path: 'my-profile' , component: { template: '<div>My Profile</div>' }},
        { path: 'my-cart'    , component: MyCart },
        { path: 'manage'     , redirect:  '/manage/dashboard', component: { render(c) { return c('router-view') } },
          children: [
            { path: 'dashboard' , component: Dashboard },
            { path: 'users'     , component: User      },
            { path: 'customers' , component: Customer  },
            { path: 'orders'    , component: Order     },
            { path: 'products'  , component: Product   },
            { path: 'employees' , component: Employee  },
            { path: 'carts'     , component: { template: '<div>Manage Carts</div>' }}
          ]
        }
      ]
    },
    // the default route, when none of the above matches:
    { path: "*", component: LoginPage , meta: { permitAll: true} }

  ]
})

router.beforeEach((to:Route, from:Route, next:Function) => {
  if (to.meta.permitAll) {
    next();
  }
  else {
    //console.log("Need Authentication: user:%s, role:%s, tokek:$s", store.state.user, store.state.role, store.state.jwt);
    if (!store.state.user || !store.state.role || !store.state.jwt ){
      next('/login')
    }
    else{
      next();
    }
  }

})

router.afterEach((to:Route, from:Route) => {
  //let navSection = appMenuUtil.search(to.path,'route');
  //store.commit('currentHeaderItem',navSection.headerItem);
})

export default router;
