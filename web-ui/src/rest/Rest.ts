import user from '@/rest/_user';
import cart from '@/rest/_cart';
import order from '@/rest/_order';
import product from '@/rest/_product';
import customer from '@/rest/_customer';
import employee from '@/rest/_employee';


//Make one large object called 'Rest' by merging all the small objects. 
const Rest = {
  ...user,
  ...cart,
  ...order,
  ...product,
  ...customer,
  ...employee
}

export default Rest;