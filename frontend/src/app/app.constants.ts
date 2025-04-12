const API_URL: string = "http://localhost:9000";

export const APP_CONSTANTS = {
  // Endpoints
  ENDPOINT_WS: API_URL + "/ws",
  ENDPOINT_AVATAR: "https://api.dicebear.com/9.x/personas/svg?radius=50&backgroundColor=ffffff&seed=",
  ENDPOINT_USER_REGISTER: API_URL + "/user",
  ENDPOINT_USER_GET: API_URL + "/user",
  ENDPOINT_USER_UPDATE: API_URL + "/user",
  ENDPOINT_USER_LOGIN: API_URL + "/user/login",
  ENDPOINT_USER_LOGIN_CHECK: API_URL + "/user/login/check",
  ENDPOINT_USER_LOGOUT: API_URL + "/user/logout",

  ENDPOINT_WALLET_GET: API_URL + "/wallet",
  ENDPOINT_WALLET_RECHARGE: API_URL + "/wallet/recharge",
  ENDPOINT_WALLET_TRANSFER: API_URL + "/wallet/transfer",
  ENDPOINT_WALLET_GET_SCHEDULE: API_URL + "/wallet/transfer/scheduler",
  ENDPOINT_WALLET_CREATE_SCHEDULE: API_URL + "/wallet/transfer/scheduler",
  ENDPOINT_WALLET_DELETE_SCHEDULE: API_URL + "/wallet/transfer/scheduler",

  // Absolute paths
  PATH_ROOT: "/",
  PATH_LOGIN: "/login",
  PATH_DASHBOARD: "/dashboard",
  PATH_TRANSFER: "/dashboard/transfer",

  // Messages
  MESSAGE_SUCCESSFUL: "Operazione eseguita con successo. Potrai visualizzare le modifiche a breve",
  MESSAGE_GENERIC_ERROR: "Errore generico",

  // Web socket
  WS_CUSTOMER_REGISTERED: "customer_registered",
  WS_WALLET_UPDATED: "wallet_updated",
  WS_SCHEDULED_OPERATIONS_UPDATED: "scheduled_operations_updated",
  WS_USER_DATA_UPDATED: "user_data_updated",

  // Misc
  PASSWORD_MIN_LENGHT: 8,
  CUSTOMER_ID_COOKIE_NAME: "easypay_customer_id"
};
