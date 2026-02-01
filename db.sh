#!/bin/bash

# SellerX Veritabanı Yönetim Scripti
# Kullanım: ./db.sh [start|stop|restart|status|logs|reset|connect]

case "$1" in
  start)
    echo "🚀 PostgreSQL başlatılıyor..."
    docker-compose -f docker-compose.db.yml up -d
    echo "✅ Veritabanı hazır: localhost:5432"
    ;;
  stop)
    echo "🛑 PostgreSQL durduruluyor..."
    docker-compose -f docker-compose.db.yml down
    echo "✅ Veritabanı durduruldu"
    ;;
  restart)
    echo "🔄 PostgreSQL yeniden başlatılıyor..."
    docker-compose -f docker-compose.db.yml restart
    echo "✅ Veritabanı yeniden başlatıldı"
    ;;
  status)
    echo "📊 Veritabanı durumu:"
    docker-compose -f docker-compose.db.yml ps
    ;;
  logs)
    echo "📜 Veritabanı logları:"
    docker-compose -f docker-compose.db.yml logs -f postgres
    ;;
  reset)
    echo "⚠️  DİKKAT: Tüm veriler silinecek!"
    read -p "Devam etmek istiyor musunuz? (y/N): " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
      docker-compose -f docker-compose.db.yml down -v
      docker-compose -f docker-compose.db.yml up -d
      echo "✅ Veritabanı sıfırlandı"
    else
      echo "❌ İptal edildi"
    fi
    ;;
  connect)
    echo "🔌 PostgreSQL'e bağlanılıyor..."
    docker exec -it sellerx-db psql -U postgres -d sellerx_db
    ;;
  *)
    echo "SellerX Veritabanı Yönetimi"
    echo ""
    echo "Kullanım: ./db.sh [komut]"
    echo ""
    echo "Komutlar:"
    echo "  start   - Veritabanını başlat"
    echo "  stop    - Veritabanını durdur"
    echo "  restart - Veritabanını yeniden başlat"
    echo "  status  - Veritabanı durumunu göster"
    echo "  logs    - Veritabanı loglarını göster"
    echo "  reset   - Veritabanını sıfırla (TÜM VERİLER SİLİNİR!)"
    echo "  connect - PostgreSQL shell'e bağlan"
    ;;
esac
