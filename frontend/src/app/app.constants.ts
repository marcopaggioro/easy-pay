import {environment} from '../environments/environment';
import {Validators} from '@angular/forms';

export const APP_CONSTANTS = {
  // Endpoints
  ENDPOINT_WS: environment.API_URL + "/ws",
  ENDPOINT_AVATAR: "https://api.dicebear.com/9.x/personas/svg?radius=50&backgroundColor=ffffff&seed=",
  ENDPOINT_USER_REGISTER: environment.API_URL + "/user",
  ENDPOINT_USER_GET: environment.API_URL + "/user",
  ENDPOINT_USER_UPDATE: environment.API_URL + "/user",
  ENDPOINT_USER_LOGIN: environment.API_URL + "/user/login",
  ENDPOINT_USER_LOGIN_CHECK: environment.API_URL + "/user/login/check",
  ENDPOINT_USER_LOGOUT: environment.API_URL + "/user/logout",
  ENDPOINT_USER_REFRESH_TOKEN: environment.API_URL + "/user/refresh-token",
  ENDPOINT_USER_CREATE_PAYMENT_CARD: environment.API_URL + "/user/payment-card",
  ENDPOINT_USER_DELETE_PAYMENT_CARD: environment.API_URL + "/user/payment-card",

  ENDPOINT_WALLET_BALANCE: environment.API_URL + "/wallet/balance",
  ENDPOINT_WALLET_OPERATIONS: environment.API_URL + "/wallet/operations",
  ENDPOINT_WALLET_GET_INTERACTED_CUSTOMERS: environment.API_URL + "/wallet/interacted-customers",
  ENDPOINT_WALLET_RECHARGE: environment.API_URL + "/wallet/recharge",
  ENDPOINT_WALLET_TRANSFER: environment.API_URL + "/wallet/transfer",
  ENDPOINT_WALLET_GET_SCHEDULE: environment.API_URL + "/wallet/transfer/scheduler",
  ENDPOINT_WALLET_CREATE_SCHEDULE: environment.API_URL + "/wallet/transfer/scheduler",
  ENDPOINT_WALLET_DELETE_SCHEDULE: environment.API_URL + "/wallet/transfer/scheduler",

  // Absolute paths
  PATH_ROOT: "/",
  PATH_LOGIN: "/login",
  PATH_REGISTER: "/register",
  PATH_DASHBOARD: "/dashboard",
  PATH_TRANSFER: "/dashboard/transfer",

  // Messages
  MESSAGE_SUCCESSFUL: "Operazione eseguita con successo: potrai visualizzare le modifiche a breve",
  MESSAGE_GENERIC_ERROR: "Errore generico: riprova più tardi",

  // Web socket
  WS_CUSTOMER_REGISTERED: "customer_registered",
  WS_WALLET_UPDATED: "wallet_updated",
  WS_SCHEDULED_OPERATIONS_UPDATED: "scheduled_operations_updated",
  WS_USER_DATA_UPDATED: "user_data_updated",

  // Intervals
  INTERVAL_REGISTRATION_CHECK: 1000,
  INTERVAL_WS_RETRY: 5000,

  // Misc
  PASSWORD_MIN_LENGHT: 8,
  CUSTOMER_ID_COOKIE_NAME: "easypay_customer_id",

  // Validators
  VALIDATOR_MIN_AMOUNT: Validators.min(0.01),
  VALIDATOR_MAX_AMOUNT: Validators.max(10000)
};
