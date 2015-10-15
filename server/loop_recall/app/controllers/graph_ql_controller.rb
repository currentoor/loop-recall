class GraphQlController < ApplicationController
  skip_before_filter :verify_authenticity_token

  def query
    query_string = params[:query]
    query = GraphQL::Query.new(GraphSchema, query_string)

    render transit: query.result
  end

  def mutation
    binding.pry
  end
end
