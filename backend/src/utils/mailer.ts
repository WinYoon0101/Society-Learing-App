import nodemailer from "nodemailer";

// 1. Khởi tạo transporter
const transporter = nodemailer.createTransport({
  host: "smtp.gmail.com",
  port: 465,
  secure: true, // true đối với cổng 465 (SSL)
  family: 4,   
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS, 
  },
  // 2. Giới hạn thời gian chờ kết nối 
  connectionTimeout: 10000, 
  greetingTimeout: 10000,
  socketTimeout: 10000,
});

export const sendOtpEmail = async (to: string, otp: string) => {
  // 3. Xây dựng giao diện HTML cho Email
  const htmlContent = `
    <div style="font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; background-color: #f9fafb; padding: 40px 20px; color: #374151;">
      <div style="max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
        
        <div style="background-color: #10B981; padding: 24px; text-align: center;">
          <h2 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 600;">Khôi phục mật khẩu</h2>
        </div>
        
        <div style="padding: 32px 24px;">
          <p style="margin-top: 0; font-size: 16px;">Chào bạn,</p>
          <p style="font-size: 16px; line-height: 1.5; color: #4b5563;">
            Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã OTP dưới đây để xác nhận:
          </p>
          
          <div style="text-align: center; margin: 32px 0;">
            <span style="display: inline-block; font-size: 32px; font-weight: 700; color: #10B981; background-color: #ecfdf5; padding: 12px 24px; border-radius: 8px; border: 1px dashed #10B981; letter-spacing: 4px;">
              ${otp}
            </span>
          </div>
          
          <p style="font-size: 14px; color: #6b7280; line-height: 1.5;">
            <strong>Lưu ý:</strong> Mã này có hiệu lực trong vòng <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn cho tài khoản của bạn.
          </p>
          <p style="font-size: 14px; color: #6b7280; line-height: 1.5; margin-bottom: 0;">
            Nếu bạn không yêu cầu đặt lại mật khẩu, xin vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.
          </p>
        </div>
        
        <div style="background-color: #f3f4f6; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;">
          <p style="margin: 0; font-size: 12px; color: #9ca3af;">
            © ${new Date().getFullYear()} Society. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  `;

  // Thực hiện gửi mail
  await transporter.sendMail({
    from: `"Society"`, 
    to,
    subject: "🔒 Mã OTP khôi phục mật khẩu", 
    html: htmlContent, 
  });
};