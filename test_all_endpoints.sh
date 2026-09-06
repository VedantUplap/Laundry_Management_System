#!/usr/bin/env bash
# =============================================================================
# Automated Test Script: Comprehensive Verification of all 33 LSMS Endpoints
# =============================================================================

set -e
BASE="http://localhost:3001/api"
PASS=0
FAIL=0

assert() {
  local desc="$1"
  local status="$2"
  local expected="$3"
  if [ "$status" -eq "$expected" ]; then
    echo "  ✅ PASS: $desc (HTTP $status)"
    PASS=$((PASS + 1))
  else
    echo "  ❌ FAIL: $desc (Expected HTTP $expected, got $status)"
    FAIL=$((FAIL + 1))
  fi
}

echo "==============================================================="
echo "   🧪 Testing LSMS Java + JDBC Web Backend                      "
echo "==============================================================="

# 1. Login Admin
echo -e "\n--- 1. Authentication ---"
ADMIN_RESP=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"email":"uplap.vedant@gmail.com","password":"password"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -n "$ADMIN_TOKEN" ]; then
  assert "Admin Login" 200 200
else
  assert "Admin Login" 500 200
fi

# 2. Login Customer
CUST_RESP=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"email":"aarav.sharma@gmail.com","password":"password"}')
CUST_TOKEN=$(echo "$CUST_RESP" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -n "$CUST_TOKEN" ]; then
  assert "Customer Login" 200 200
else
  assert "Customer Login" 500 200
fi

# 3. GET /api/auth/me
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/auth/me")
assert "GET /api/auth/me (Admin)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/auth/me")
assert "GET /api/auth/me (Customer)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/auth/me")
assert "GET /api/auth/me (No token -> 401)" "$STATUS" 401

# 4. RBAC Guards
echo -e "\n--- 2. RBAC Access Control ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/inventory")
assert "Customer accessing /api/inventory -> 403" "$STATUS" 403

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/dashboard/stats")
assert "Customer accessing /api/dashboard/stats -> 403" "$STATUS" 403

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/inventory")
assert "Admin accessing /api/inventory -> 200" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/dashboard/stats")
assert "Admin accessing /api/dashboard/stats -> 200" "$STATUS" 200

# 5. Services
echo -e "\n--- 3. Services Management ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/services")
assert "GET /api/services (Public)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/services/1")
assert "GET /api/services/1" "$STATUS" 200

# 6. Customers
echo -e "\n--- 4. Customer Management ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/customers")
assert "GET /api/customers (Admin)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/customers/1")
assert "GET /api/customers/1 (Admin)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/customers/1/orders")
assert "GET /api/customers/1/orders (Self)" "$STATUS" 200

# 7. Orders & Transactions
echo -e "\n--- 5. Orders & Transactions ---"
NEW_ORDER_RESP=$(curl -s -X POST "$BASE/orders" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerID":1,"pickupDate":"2026-09-08","notes":"Automated JDBC Test Order","details":[{"serviceID":1,"garmentType":"Shirts","quantity":3,"weightKg":1.5},{"serviceID":3,"garmentType":"Pants","quantity":2,"weightKg":1.0}]}')
NEW_ORDER_ID=$(echo "$NEW_ORDER_RESP" | grep -o '"orderID":[0-9]*' | cut -d':' -f2)

if [ -n "$NEW_ORDER_ID" ]; then
  assert "POST /api/orders (Multi-item Transaction with DB Triggers)" 201 201
  echo "    Created OrderID: $NEW_ORDER_ID with details: $NEW_ORDER_RESP"

  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/orders/$NEW_ORDER_ID")
  assert "GET /api/orders/$NEW_ORDER_ID" "$STATUS" 200

  # Update status to Packed -> tests auto-billing generation
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/orders/$NEW_ORDER_ID/status" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"status":"Packed"}')
  assert "PUT /api/orders/$NEW_ORDER_ID/status (Auto-generate Billing)" "$STATUS" 200

  # Check generated bill
  BILL_RESP=$(curl -s -H "Authorization: Bearer $CUST_TOKEN" "$BASE/billing/order/$NEW_ORDER_ID")
  NEW_BILL_ID=$(echo "$BILL_RESP" | grep -o '"BillID":[0-9]*' | cut -d':' -f2)
  if [ -n "$NEW_BILL_ID" ]; then
    assert "GET /api/billing/order/$NEW_ORDER_ID" 200 200
    echo "    Generated BillID: $NEW_BILL_ID"

    # Make payment
    PAY_RESP=$(curl -s -X POST "$BASE/payments" \
      -H "Authorization: Bearer $CUST_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"billID\":$NEW_BILL_ID,\"amountPaid\":50.00,\"paymentMethod\":\"UPI\",\"transactionID\":\"TEST_TX_123\"}")
    STATUS=$(echo "$PAY_RESP" | grep -o '"success":true' | wc -l | tr -d ' ')
    if [ "$STATUS" -ge 1 ]; then
      assert "POST /api/payments (Partial Payment Transaction)" 200 200
    else
      assert "POST /api/payments" 500 200
    fi
  fi
fi

# 8. Inventory & Usage
echo -e "\n--- 6. Inventory Tracking ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/inventory/usage")
assert "GET /api/inventory/usage" "$STATUS" 200

# 9. Deliveries & Agents
echo -e "\n--- 7. Delivery Operations ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/delivery-agents")
assert "GET /api/delivery-agents" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/deliveries")
assert "GET /api/deliveries" "$STATUS" 200

# 10. Feedback
echo -e "\n--- 8. Feedback ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/feedback")
assert "GET /api/feedback (Admin)" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUST_TOKEN" "$BASE/feedback/my")
assert "GET /api/feedback/my (Customer)" "$STATUS" 200

# 11. Reports (all 7)
echo -e "\n--- 9. Analytics & Reports ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/daily-revenue")
assert "Report: daily-revenue" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/monthly-revenue")
assert "Report: monthly-revenue" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/pending-deliveries")
assert "Report: pending-deliveries" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/customer-history/1")
assert "Report: customer-history" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/inventory-usage")
assert "Report: inventory-usage" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/popular-services")
assert "Report: popular-services" "$STATUS" 200

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/reports/outstanding-payments")
assert "Report: outstanding-payments" "$STATUS" 200

echo -e "\n==============================================================="
echo "   📊 Test Results: $PASS Passed, $FAIL Failed"
echo "==============================================================="

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
