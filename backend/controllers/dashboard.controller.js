const db = require('../config/db');

// GET /api/dashboard/stats
const getDashboardStats = async (req, res) => {
  try {
    const [[customers]]  = await db.query('SELECT COUNT(*) AS total FROM CUSTOMER');
    const [[orders]]     = await db.query('SELECT COUNT(*) AS total FROM ORDERS');
    const [[pending]]    = await db.query("SELECT COUNT(*) AS total FROM ORDERS WHERE Status = 'Pending'");
    const [[processing]] = await db.query(
      "SELECT COUNT(*) AS total FROM ORDERS WHERE Status IN ('Processing','Washing','Drying','Ironing','Picked Up','Received')"
    );
    const [[completed]]  = await db.query("SELECT COUNT(*) AS total FROM ORDERS WHERE Status = 'Delivered'");
    const [[pendingDel]] = await db.query(
      "SELECT COUNT(*) AS total FROM DELIVERY WHERE DeliveryStatus NOT IN ('Delivered','Failed')"
    );
    const [[revenue]]    = await db.query('SELECT COALESCE(SUM(AmountPaid), 0) AS total FROM PAYMENT');
    const [[outstanding]]= await db.query(
      `SELECT COALESCE(SUM(b.TotalAmount - COALESCE(p.paid,0)), 0) AS total
       FROM BILLING b
       LEFT JOIN (SELECT BillID, SUM(AmountPaid) AS paid FROM PAYMENT GROUP BY BillID) p ON p.BillID = b.BillID
       WHERE b.BillStatus != 'Paid'`
    );
    const [[lowStock]]   = await db.query(
      'SELECT COUNT(*) AS total FROM INVENTORY WHERE QuantityAvailable <= ReorderLevel'
    );
    const [popularSvc]   = await db.query(
      `SELECT st.ServiceName, COUNT(od.DetailID) AS orderCount
       FROM SERVICE_TYPE st
       LEFT JOIN ORDER_DETAILS od ON od.ServiceID = st.ServiceID
       GROUP BY st.ServiceID ORDER BY orderCount DESC LIMIT 1`
    );
    const [[avgRating]]  = await db.query('SELECT ROUND(AVG(Rating),1) AS avg FROM FEEDBACK');

    res.json({
      success: true,
      data: {
        totalCustomers:      customers.total,
        totalOrders:         orders.total,
        pendingOrders:       pending.total,
        processingOrders:    processing.total,
        completedOrders:     completed.total,
        pendingDeliveries:   pendingDel.total,
        totalRevenue:        parseFloat(revenue.total),
        outstandingPayments: parseFloat(outstanding.total),
        lowStockItems:       lowStock.total,
        mostPopularService:  popularSvc[0]?.ServiceName || 'N/A',
        averageRating:       avgRating.avg || 0,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'Failed to fetch dashboard stats.' });
  }
};

module.exports = { getDashboardStats };
