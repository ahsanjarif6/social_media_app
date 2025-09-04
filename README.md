# 📱 Social Media App (Android + Supabase)

A **Reddit-style social media application** built with **Kotlin (Android)** and **Supabase**.  
Users can register with email (with confirmation), update profiles, upload posts (images/videos), vote, comment, and chat in real-time via **WebSockets**.

---

## ✨ Features

### 🔑 Authentication
- Register/login using **Supabase Auth**.
- Email confirmation required — users receive a **verification email** upon registration.
- Secure login sessions handled with Supabase SDK.

### 👤 User Profiles
- Upload/change **profile picture**.
- Manage personal details.

### 📝 Posts
- Create posts with **images or videos**.
- Reddit-style **upvote/downvote** system.
- Posts appear in **followers’ feed**.
- Real-time feed updates when followed users create posts.

### 💬 Comments
- Add comments to posts.
- Each comment supports **upvotes and downvotes**.
- Real-time updates when new comments are added.

### 📩 Messaging
- **1-to-1 chat system** implemented with **WebSockets**.
- Messages are persisted in Supabase (Postgres DB).
- Real-time delivery (sender + receiver see updates instantly).
- Unread message badge on bottom navigation.

### 🔔 Notifications (Realtime)
- Feed and chat update instantly using **Supabase Realtime channels**.
- Unfollowed users’ posts are automatically removed from the feed.

---

## 🛠️ Tech Stack

### Android
- **Kotlin + Android SDK**
- **RecyclerView + ViewBinding**
- **Glide** (image loading)
- **ExoPlayer** (video playback)
- **Coroutines + Flow** (async + realtime updates)
- **OkHttp WebSocket** (chat system)

### Backend
- **Supabase** (Postgres, Auth, Realtime, Storage)
- **Supabase Realtime (channels)** for posts & comments.
- **Node.js (WebSocket server)** for messaging persistence.

### Database (Supabase Postgres)
- `users` – authentication & profile.
- `posts` – user posts (text, images, videos).
- `comments` – nested comments with votes.
- `votes` – post & comment voting.
- `follows` – following system.
- `messages` – chat storage.

---

## 🗄️ Database Schema

Schema generated from Supabase:

![Database Schema](docs/supabase-schema-xbkzzmzlcvwfprfdlulp.svg)

> The schema above shows relationships between `users`, `posts`, `comments`, `votes`, `follows`, and `messages`.

---

## 🎥 Demo

Watch the demo here:  
👉 [Demo Video](https://drive.google.com/file/d/123lQ1NEJxYk0IktWgrXPvcvKjBkGqRpF/view?usp=sharing) *(replace with actual path/link)*

---

## 🚀 Getting Started

### 1️⃣ Prerequisites
- Android Studio **Giraffe+**
- Node.js **20+**
- Supabase account ([https://supabase.com](https://supabase.com))

### 2️⃣ Clone Repository
```bash
git clone https://github.com/yourusername/social-media-app.git
cd social-media-app
