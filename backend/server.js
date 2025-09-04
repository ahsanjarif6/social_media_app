

require('dotenv').config();
const { createClient } = require("@supabase/supabase-js");

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_KEY
);

const WebSocket = require("ws");
const wss = new WebSocket.Server({ port: process.env.PORT });

const userConnections = new Map();

wss.on("connection", (socket) => {
  console.log("New client connected");
  
  socket.on("message", async (raw) => {
    try {
      const msg = JSON.parse(raw);
      
      if (msg.type === "register") {
        const { userId } = msg;
        userConnections.set(userId, socket);
        socket.userId = userId;
        console.log(`User ${userId} registered`);
        
        socket.send(JSON.stringify({ 
          type: "register_success", 
          message: "Registered successfully" 
        }));
        return;
      }
      
      if (msg.type === "message") {
        const { chatId, senderId, receiverId, content } = msg;
        
        try {
          const { data, error } = await supabase
            .from("messages")
            .insert([{ 
              chat_id: chatId, 
              sender_id: senderId, 
              receiver_id: receiverId, 
              content 
            }])
            .select("*");
          
          if (error) {
            console.error("Supabase insert error:", error.message);
            socket.send(JSON.stringify({ 
              type: "error", 
              message: error.message 
            }));
            return;
          }
          
          const savedMessage = data[0];
          
          const payload = JSON.stringify({
            type: "message",
            id: savedMessage.id,
            chatId: savedMessage.chat_id,
            senderId: savedMessage.sender_id,
            receiverId: savedMessage.receiver_id,
            content: savedMessage.content,
            createdAt: savedMessage.created_at,
            read: savedMessage.read || false
          });
          
          const senderSocket = userConnections.get(senderId);
          const receiverSocket = userConnections.get(receiverId);
          
          if (senderSocket && senderSocket.readyState === WebSocket.OPEN) {
            senderSocket.send(payload);
            console.log(`Message sent to sender ${senderId}`);
          }
          
          if (receiverSocket && receiverSocket.readyState === WebSocket.OPEN) {
            receiverSocket.send(payload);
            console.log(`Message sent to receiver ${receiverId}`);
          } else {
            console.log(`Receiver ${receiverId} is not online`);
          }
          
        } catch (err) {
          console.error("Unexpected error:", err);
          socket.send(JSON.stringify({ 
            type: "error", 
            message: "Failed to process message" 
          }));
        }
      }
      
    } catch (err) {
      console.error("Invalid message:", err.message);
      socket.send(JSON.stringify({ 
        type: "error", 
        message: "Invalid message format" 
      }));
    }
  });
  
  socket.on("close", () => {
    if (socket.userId) {
      userConnections.delete(socket.userId);
      console.log(`User ${socket.userId} disconnected`);
    }
  });
  
  socket.on("error", (error) => {
    console.error("WebSocket error:", error);
    if (socket.userId) {
      userConnections.delete(socket.userId);
    }
  });
});

console.log("WebSocket server running on port " + process.env.PORT);
console.log("Connected users will be tracked in userConnections Map");