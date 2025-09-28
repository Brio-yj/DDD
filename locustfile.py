from locust import HttpUser, task, between


class BaselineUser(HttpUser):
    """Baseline traffic that checks health and product lookup."""

    host = "http://localhost:8084"
    wait_time = between(1, 3)

    @task
    def browse_products(self):
        self.client.get("/health")
        self.client.get("/product/1")


class SessionAttackUser(HttpUser):
    """Aggressive login attempts against the session auth server."""

    host = "http://localhost:8081"
    wait_time = between(0.1, 0.5)

    @task
    def login_spam(self):
        self.client.post("/login", json={"username": "alice", "password": "wrong"})
        self.client.post("/login", json={"username": "alice", "password": "password"})


class JwtAndOrderUser(HttpUser):
    """Login to JWT server and place an order using the token."""

    host = "http://localhost:8083"
    wait_time = between(0.5, 1.5)

    def on_start(self):
        self.token = None

    def ensure_token(self):
        if self.token is None:
            response = self.client.post(
                "http://localhost:8082/login",
                json={"username": "bob", "password": "password"},
                name="jwt_login",
            )
            if response.status_code == 200:
                self.token = response.json().get("token")
            else:
                self.token = None
        return self.token

    @task
    def login_and_order(self):
        token = self.ensure_token()
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        response = self.client.post(
            "/order",
            json={"productId": 1, "quantity": 1},
            headers=headers,
            name="order_with_jwt",
        )
        if response.status_code == 401:
            self.token = None
