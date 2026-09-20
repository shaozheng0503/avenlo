"""Avenlo Mock Server —— Anker 黑客松 Hotfix 队
Idea Card V2.1 契约的参考实现（M1 冻结基准），App 端与后端从这里对齐。

启动:
    uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
契约文档:
    http://localhost:8000/docs
"""
fastapi==0.115.0
uvicorn[standard]==0.30.6
