const API_URL: string = "http://localhost:9000";

export const APP_CONSTANTS = {
  // Endpoints
  AVATAR_ENDPOINT: "https://api.dicebear.com/9.x/personas/svg?radius=50&backgroundColor=ffffff&seed=",
  USER_REGISTER_ENDPOINT: API_URL + "/user",
  USER_GET_ENDPOINT: API_URL + "/user",
  USER_UPDATE_ENDPOINT: API_URL + "/user",
  USER_LOGIN_ENDPOINT: API_URL + "/user/login",
  USER_LOGIN_CHECK_ENDPOINT: API_URL + "/user/login/check",
  USER_LOGOUT_ENDPOINT: API_URL + "/user/logout",

  WALLET_GET_ENDPOINT: API_URL + "/wallet",
  WALLET_RECHARGE_ENDPOINT: API_URL + "/wallet/recharge",
  WALLET_TRANSFER_ENDPOINT: API_URL + "/wallet/transfer",
  WALLET_GET_SCHEDULE_ENDPOINT: API_URL + "/wallet/transfer/scheduler",
  WALLET_CREATE_SCHEDULE_ENDPOINT: API_URL + "/wallet/transfer/scheduler",
  WALLET_DELETE_SCHEDULE_ENDPOINT: API_URL + "/wallet/transfer/scheduler",

  // Absolute paths
  PATH_ROOT: "/",
  PATH_LOGIN: "/login",
  PATH_DASHBOARD: "/dashboard",

  // Messages
  MESSAGE_SUCCESSFUL: "Operazione andata a buon fine",
  MESSAGE_GENERIC_ERROR: "Errore generico",

  // Misc
  PASSWORD_MIN_LENGHT: 8
};
