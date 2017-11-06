(ns widgetshop.app.products
  "Controls product listing information."
  (:require [widgetshop.app.state :as state]
            [widgetshop.server :as server]))

(defn- products-by-category [app category products]
  (assoc-in app [:products-by-category category] products))

(defn- set-categories [app categories]
  (assoc-in app [:categories] categories))

(defn- load-products-by-category! [{:keys [categories] :as app} server-get-fn! category-id]
  (let [category (some #(when (= (:id %) category-id) %) categories)]
    (server-get-fn! category)
    (-> app
        (assoc :category category)
        (assoc-in [:products-by-category category] :loading))))

(defn select-category-by-id! [category-id]
  (state/update-state!
    load-products-by-category!
    (fn [category]
      (server/get! (str "/products/" (:id category))
                   {:on-success #(state/update-state! products-by-category category %)}))
    category-id))

(defn load-product-categories! []
  (server/get! "/categories" {:on-success #(state/update-state! set-categories %)}))

(defn add-to-cart! [product]
  (state/update-state! (fn [app]
                         (assoc app :cart (conj (:cart app) product)))))

(defn find-product-by-index [app idx]
  (let [category (:category app)
        products-of-category ((:products-by-category app) category)
        product-by-index (nth products-of-category idx)]
    product-by-index))

(defn set-product [app product-idx]
  (assoc app :selected-item (find-product-by-index app product-idx)))

(defn select-product! [product-idx]
  (state/update-state! set-product product-idx))

(defn unselect-product! []
  (state/update-state! dissoc :selected-item))
