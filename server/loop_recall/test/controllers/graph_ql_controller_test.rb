require 'test_helper'

class GraphQlControllerTest < ActionController::TestCase
  test "should get query" do
    get :query
    assert_response :success
  end

  test "should get mutation" do
    get :mutation
    assert_response :success
  end

end
