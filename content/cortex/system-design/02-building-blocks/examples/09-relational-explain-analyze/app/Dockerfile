FROM python:3.11-alpine

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY explain_demo.py .

CMD ["python", "explain_demo.py"]
